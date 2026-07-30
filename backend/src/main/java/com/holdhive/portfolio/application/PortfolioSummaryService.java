package com.holdhive.portfolio.application;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.holdhive.portfolio.domain.HoldingValuationInput;
import com.holdhive.portfolio.domain.PortfolioCalculator;
import com.holdhive.portfolio.domain.PortfolioValuation;
import com.holdhive.pricing.application.PriceMode;
import com.holdhive.pricing.application.PricingService;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

@Service
public class PortfolioSummaryService {

    private static final BigDecimal FIXED_PRICE = BigDecimal.ONE;

    private final PortfolioHoldingReader holdingReader;
    private final PricingService pricingService;
    private final PortfolioCalculator portfolioCalculator;

    public PortfolioSummaryService(
        PortfolioHoldingReader holdingReader,
        PricingService pricingService,
        PortfolioCalculator portfolioCalculator
    ) {
        this.holdingReader = Objects.requireNonNull(holdingReader, "holdingReader must not be null");
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService must not be null");
        this.portfolioCalculator = Objects.requireNonNull(portfolioCalculator, "portfolioCalculator must not be null");
    }

    public PortfolioSummary getSummary(PriceMode priceMode) {
        PriceMode resolvedPriceMode = priceMode == null ? PriceMode.BEST_AVAILABLE : priceMode;
        PortfolioSnapshot snapshot = holdingReader.findDefaultPortfolio();
        List<String> providerQuoteIds = snapshot.holdings().stream()
            .filter(holding -> !holding.assetType().isFixedValueAsset())
            .map(HoldingPosition::providerQuoteId)
            .toList();
        Map<String, MarketQuote> quotesById = fetchQuotes(providerQuoteIds);

        List<HoldingValuationInput> valuationInputs = snapshot.holdings().stream()
            .map(holding -> toValuationInput(holding, quotesById.get(normalize(holding.providerQuoteId())), resolvedPriceMode))
            .toList();

        PortfolioValuation valuation = portfolioCalculator.calculate(valuationInputs);
        return PortfolioSummary.from(snapshot, valuation);
    }

    private Map<String, MarketQuote> fetchQuotes(List<String> providerQuoteIds) {
        if (providerQuoteIds.isEmpty()) {
            return Map.of();
        }
        return pricingService.getQuotes(providerQuoteIds).stream()
            .collect(Collectors.toMap(
                quote -> normalize(quote.providerQuoteId()),
                Function.identity(),
                (first, ignored) -> first
            ));
    }

    private HoldingValuationInput toValuationInput(
        HoldingPosition holding,
        MarketQuote quote,
        PriceMode priceMode
    ) {
        if (holding.assetType().isFixedValueAsset()) {
            return new HoldingValuationInput(
                holding.holdingId(),
                holding.assetType(),
                holding.ticker(),
                holding.quantity(),
                holding.averagePurchasePrice(),
                FIXED_PRICE,
                PriceStatus.FIXED,
                null
            );
        }
        MarketQuote acceptedQuote = acceptQuote(quote, priceMode);
        return new HoldingValuationInput(
            holding.holdingId(),
            holding.assetType(),
            holding.ticker(),
            holding.quantity(),
            holding.averagePurchasePrice(),
            acceptedQuote == null ? null : acceptedQuote.currentPrice(),
            acceptedQuote == null ? PriceStatus.UNAVAILABLE : acceptedQuote.priceStatus(),
            acceptedQuote == null ? null : acceptedQuote.priceObservedAt()
        );
    }

    private static MarketQuote acceptQuote(MarketQuote quote, PriceMode priceMode) {
        if (quote == null) {
            return null;
        }
        if (priceMode == PriceMode.LIVE_ONLY && quote.priceStatus() != PriceStatus.LIVE) {
            return null;
        }
        if (priceMode != PriceMode.DEMO_ALLOWED && quote.priceStatus() == PriceStatus.DEMO) {
            return null;
        }
        return quote;
    }

    private static String normalize(String providerQuoteId) {
        return providerQuoteId == null ? "" : providerQuoteId.trim().toUpperCase(Locale.ROOT);
    }
}
