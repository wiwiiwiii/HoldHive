package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.PriceMode;
import com.holdhive.pricing.application.PricingService;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

@Service
public class PortfolioExposureService {

    private static final int SCALE = 8;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal FIXED_PRICE = BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP);

    private final PortfolioHoldingReader holdingReader;
    private final PricingService pricingService;
    private final FundLookthroughService fundLookthroughService;

    public PortfolioExposureService(
        PortfolioHoldingReader holdingReader,
        PricingService pricingService,
        FundLookthroughService fundLookthroughService
    ) {
        this.holdingReader = Objects.requireNonNull(holdingReader, "holdingReader must not be null");
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService must not be null");
        this.fundLookthroughService = Objects.requireNonNull(
            fundLookthroughService,
            "fundLookthroughService must not be null"
        );
    }

    public PortfolioExposure getExposure(boolean lookthrough, PriceMode priceMode) {
        PriceMode resolvedPriceMode = priceMode == null ? PriceMode.BEST_AVAILABLE : priceMode;
        PortfolioSnapshot snapshot = holdingReader.findDefaultPortfolio();
        Map<String, MarketQuote> quotesById = fetchQuotes(snapshot);
        Map<String, ExposureAccumulator> exposureByTicker = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        BigDecimal totalMarketValue = BigDecimal.ZERO;

        for (HoldingPosition holding : snapshot.holdings()) {
            BigDecimal marketValue = marketValue(holding, quotesById.get(normalize(holding.providerQuoteId())), resolvedPriceMode);
            if (marketValue == null) {
                warnings.add(holding.ticker() + " has no available price and is excluded from exposure.");
                continue;
            }
            totalMarketValue = totalMarketValue.add(marketValue);

            if (lookthrough && holding.assetType().isFundLike()) {
                applyFundLookthrough(holding, marketValue, exposureByTicker, warnings);
            } else {
                addExposure(
                    exposureByTicker,
                    holding.ticker(),
                    holding.ticker(),
                    holding.assetType(),
                    marketValue,
                    BigDecimal.ZERO,
                    "DIRECT"
                );
            }
        }

        BigDecimal scaledTotalMarketValue = scale(totalMarketValue);
        List<PortfolioExposureItem> items = exposureByTicker.values().stream()
            .map(accumulator -> accumulator.toItem(scaledTotalMarketValue))
            .sorted(Comparator.comparing(PortfolioExposureItem::totalExposureValue).reversed())
            .toList();

        items.stream()
            .filter(item -> item.directMarketValue().compareTo(BigDecimal.ZERO) > 0
                && item.fundLookthroughMarketValue().compareTo(BigDecimal.ZERO) > 0)
            .map(item -> item.ticker() + " appears both as direct holding and inside fund holdings.")
            .forEach(warnings::add);

        return new PortfolioExposure(
            snapshot.portfolioId(),
            snapshot.portfolioName(),
            snapshot.baseCurrency(),
            lookthrough,
            resolvedPriceMode,
            scaledTotalMarketValue,
            items,
            warnings.stream().distinct().toList()
        );
    }

    private Map<String, MarketQuote> fetchQuotes(PortfolioSnapshot snapshot) {
        List<String> providerQuoteIds = snapshot.holdings().stream()
            .filter(holding -> !holding.assetType().isFixedValueAsset())
            .map(HoldingPosition::providerQuoteId)
            .filter(providerQuoteId -> providerQuoteId != null && !providerQuoteId.isBlank())
            .toList();
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

    private void applyFundLookthrough(
        HoldingPosition holding,
        BigDecimal fundMarketValue,
        Map<String, ExposureAccumulator> exposureByTicker,
        List<String> warnings
    ) {
        FundLookthrough lookthrough = fundLookthroughService.getLookthrough(
            holding.instrumentId(),
            holding.ticker(),
            holding.assetType()
        );
        warnings.addAll(lookthrough.warnings());
        for (FundComponent component : lookthrough.holdings()) {
            BigDecimal componentExposure = scale(fundMarketValue
                .multiply(component.weightPercent())
                .divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP));
            addExposure(
                exposureByTicker,
                component.ticker(),
                component.displayName(),
                component.assetType(),
                BigDecimal.ZERO,
                componentExposure,
                "FUND:" + holding.ticker()
            );
        }

        BigDecimal residualPercent = ONE_HUNDRED.subtract(lookthrough.coveragePercent());
        if (residualPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal residualValue = scale(fundMarketValue
                .multiply(residualPercent)
                .divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP));
            addExposure(
                exposureByTicker,
                holding.ticker() + "_UNDISCLOSED",
                holding.ticker() + " undisclosed residual",
                holding.assetType(),
                BigDecimal.ZERO,
                residualValue,
                "FUND:" + holding.ticker() + ":UNDISCLOSED"
            );
        }
    }

    private static void addExposure(
        Map<String, ExposureAccumulator> exposureByTicker,
        String ticker,
        String displayName,
        AssetType assetType,
        BigDecimal directMarketValue,
        BigDecimal fundLookthroughMarketValue,
        String source
    ) {
        String key = normalize(ticker);
        exposureByTicker.computeIfAbsent(
            key,
            ignored -> new ExposureAccumulator(key, displayName, assetType)
        ).add(directMarketValue, fundLookthroughMarketValue, source);
    }

    private static BigDecimal marketValue(
        HoldingPosition holding,
        MarketQuote quote,
        PriceMode priceMode
    ) {
        if (holding.assetType().isFixedValueAsset()) {
            return scale(holding.quantity().multiply(FIXED_PRICE));
        }
        MarketQuote acceptedQuote = acceptQuote(quote, priceMode);
        return acceptedQuote == null ? null : scale(holding.quantity().multiply(acceptedQuote.currentPrice()));
    }

    private static MarketQuote acceptQuote(MarketQuote quote, PriceMode priceMode) {
        if (quote == null || quote.currentPrice() == null) {
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

    private static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return numerator.multiply(ONE_HUNDRED).divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static final class ExposureAccumulator {
        private final String ticker;
        private final String displayName;
        private final AssetType assetType;
        private BigDecimal directMarketValue = BigDecimal.ZERO;
        private BigDecimal fundLookthroughMarketValue = BigDecimal.ZERO;
        private final List<String> sources = new ArrayList<>();

        private ExposureAccumulator(String ticker, String displayName, AssetType assetType) {
            this.ticker = ticker;
            this.displayName = displayName;
            this.assetType = assetType;
        }

        private void add(
            BigDecimal directMarketValue,
            BigDecimal fundLookthroughMarketValue,
            String source
        ) {
            this.directMarketValue = this.directMarketValue.add(directMarketValue);
            this.fundLookthroughMarketValue = this.fundLookthroughMarketValue.add(fundLookthroughMarketValue);
            this.sources.add(source);
        }

        private PortfolioExposureItem toItem(BigDecimal totalMarketValue) {
            BigDecimal totalExposureValue = scale(directMarketValue.add(fundLookthroughMarketValue));
            return new PortfolioExposureItem(
                ticker,
                displayName,
                assetType,
                scale(directMarketValue),
                scale(fundLookthroughMarketValue),
                totalExposureValue,
                percentage(totalExposureValue, totalMarketValue),
                sources.stream().distinct().toList()
            );
        }
    }
}
