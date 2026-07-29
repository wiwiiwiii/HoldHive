package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.holdhive.common.error.ApiException;
import com.holdhive.portfolio.persistence.entity.HoldingEntity;
import com.holdhive.portfolio.persistence.entity.InstrumentEntity;
import com.holdhive.portfolio.persistence.entity.PortfolioEntity;
import com.holdhive.portfolio.persistence.repository.HoldingRepository;
import com.holdhive.portfolio.persistence.repository.PortfolioRepository;
import com.holdhive.pricing.application.PriceMode;
import com.holdhive.pricing.application.PricingService;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

@Service
public class HoldingQueryService {

    private static final int SCALE = 8;
    private static final BigDecimal FIXED_PRICE = BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final PricingService pricingService;

    public HoldingQueryService(
        PortfolioRepository portfolioRepository,
        HoldingRepository holdingRepository,
        PricingService pricingService
    ) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public HoldingList listHoldings(String sort, PriceMode priceMode) {
        PortfolioEntity portfolio = portfolioRepository.findFirstByOrderByIdAsc()
            .orElse(null);
        if (portfolio == null) {
            return new HoldingList(List.of(), 0);
        }

        List<HoldingEntity> holdings = holdingRepository.findByPortfolioId(portfolio.getId());
        List<HoldingView> views = enrich(holdings, priceMode);
        return new HoldingList(sort(views, sort), views.size());
    }

    @Transactional(readOnly = true)
    public HoldingView getHolding(Long holdingId, PriceMode priceMode) {
        HoldingEntity requested = holdingRepository.findWithPortfolioAndInstrumentById(holdingId)
            .orElseThrow(() -> notFound(holdingId));
        List<HoldingEntity> portfolioHoldings = holdingRepository.findByPortfolioId(requested.getPortfolio().getId());
        return enrich(portfolioHoldings, priceMode).stream()
            .filter(holding -> holding.id().equals(holdingId))
            .findFirst()
            .orElseThrow(() -> notFound(holdingId));
    }

    private List<HoldingView> enrich(List<HoldingEntity> holdings, PriceMode priceMode) {
        PriceMode resolvedPriceMode = priceMode == null ? PriceMode.BEST_AVAILABLE : priceMode;
        List<String> providerQuoteIds = holdings.stream()
            .map(HoldingEntity::getInstrument)
            .filter(instrument -> !instrument.getAssetType().isFixedValueAsset())
            .map(InstrumentEntity::getProviderQuoteId)
            .filter(providerQuoteId -> providerQuoteId != null && !providerQuoteId.isBlank())
            .map(HoldingQueryService::normalizeQuoteId)
            .distinct()
            .toList();
        Map<String, MarketQuote> quotesById = fetchQuotes(providerQuoteIds);

        List<PendingHoldingView> pendingViews = holdings.stream()
            .map(holding -> toPendingView(
                holding,
                quotesById.get(normalizeQuoteId(holding.getInstrument().getProviderQuoteId())),
                resolvedPriceMode
            ))
            .toList();
        BigDecimal totalMarketValue = pendingViews.stream()
            .map(PendingHoldingView::marketValue)
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pendingViews.stream()
            .map(pending -> pending.toHoldingView(totalMarketValue))
            .toList();
    }

    private Map<String, MarketQuote> fetchQuotes(List<String> providerQuoteIds) {
        if (providerQuoteIds.isEmpty()) {
            return Map.of();
        }
        return pricingService.getQuotes(providerQuoteIds).stream()
            .collect(Collectors.toMap(
                quote -> normalizeQuoteId(quote.providerQuoteId()),
                Function.identity(),
                (first, ignored) -> first
            ));
    }

    private PendingHoldingView toPendingView(
        HoldingEntity holding,
        MarketQuote quote,
        PriceMode priceMode
    ) {
        InstrumentEntity instrument = holding.getInstrument();
        BigDecimal costBasis = scale(holding.getQuantity().multiply(holding.getAveragePurchasePrice()));
        MarketQuote acceptedQuote = acceptQuote(quote, priceMode);
        BigDecimal currentPrice = null;
        PriceStatus priceStatus = PriceStatus.UNAVAILABLE;

        if (instrument.getAssetType().isFixedValueAsset()) {
            currentPrice = FIXED_PRICE;
            priceStatus = PriceStatus.FIXED;
        } else if (acceptedQuote != null) {
            currentPrice = scale(acceptedQuote.currentPrice());
            priceStatus = acceptedQuote.priceStatus();
        }

        BigDecimal marketValue = currentPrice == null
            ? null
            : scale(holding.getQuantity().multiply(currentPrice));
        BigDecimal unrealizedGainLoss = marketValue == null
            ? null
            : scale(marketValue.subtract(costBasis));
        BigDecimal unrealizedGainLossPercent = unrealizedGainLoss == null
            ? null
            : percentageOrNull(unrealizedGainLoss, costBasis);

        return new PendingHoldingView(
            holding.getId(),
            instrument.getId(),
            instrument.getTicker(),
            instrument.getExchangeCode(),
            instrument.getDisplayName(),
            instrument.getAssetType(),
            instrument.getProvider(),
            instrument.getProviderQuoteId(),
            instrument.getCurrency(),
            scale(holding.getQuantity()),
            scale(holding.getAveragePurchasePrice()),
            currentPrice,
            marketValue,
            costBasis,
            unrealizedGainLoss,
            unrealizedGainLossPercent,
            priceStatus,
            acceptedQuote == null ? null : acceptedQuote.priceObservedAt(),
            holding.getCreatedAt(),
            holding.getUpdatedAt()
        );
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

    private static List<HoldingView> sort(List<HoldingView> views, String sort) {
        SortSpec sortSpec = SortSpec.parse(sort);
        Comparator<HoldingView> comparator = switch (sortSpec.field()) {
            case "marketValue" -> nullableBigDecimalComparator(HoldingView::marketValue, sortSpec.descending());
            case "unrealizedGainLoss" -> nullableBigDecimalComparator(
                HoldingView::unrealizedGainLoss,
                sortSpec.descending()
            );
            default -> Comparator.comparing(HoldingView::ticker, String.CASE_INSENSITIVE_ORDER);
        };
        if (sortSpec.descending() && "ticker".equals(sortSpec.field())) {
            comparator = comparator.reversed();
        }
        return views.stream()
            .sorted(comparator.thenComparing(HoldingView::id))
            .toList();
    }

    private static Comparator<HoldingView> nullableBigDecimalComparator(
        Function<HoldingView, BigDecimal> valueExtractor,
        boolean descending
    ) {
        return (left, right) -> {
            BigDecimal leftValue = valueExtractor.apply(left);
            BigDecimal rightValue = valueExtractor.apply(right);
            if (leftValue == null && rightValue == null) {
                return 0;
            }
            if (leftValue == null) {
                return 1;
            }
            if (rightValue == null) {
                return -1;
            }
            int result = leftValue.compareTo(rightValue);
            return descending ? -result : result;
        };
    }

    private static String normalizeQuoteId(String providerQuoteId) {
        return providerQuoteId == null ? "" : providerQuoteId.trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal percentageOrNull(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator
            .multiply(ONE_HUNDRED)
            .divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static ApiException notFound(Long holdingId) {
        return new ApiException(
            HttpStatus.NOT_FOUND,
            "HOLDING_NOT_FOUND",
            "Holding " + holdingId + " was not found"
        );
    }

    private record SortSpec(String field, boolean descending) {
        static SortSpec parse(String sort) {
            if (sort == null || sort.isBlank()) {
                return new SortSpec("ticker", false);
            }
            String[] parts = sort.split(",", 2);
            String field = parts[0].trim();
            boolean descending = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());
            return new SortSpec(field, descending);
        }
    }

    private record PendingHoldingView(
        Long id,
        Long instrumentId,
        String ticker,
        String exchangeCode,
        String displayName,
        com.holdhive.portfolio.domain.AssetType assetType,
        String provider,
        String providerQuoteId,
        String currency,
        BigDecimal quantity,
        BigDecimal averagePurchasePrice,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal costBasis,
        BigDecimal unrealizedGainLoss,
        BigDecimal unrealizedGainLossPercent,
        PriceStatus priceStatus,
        java.time.Instant priceObservedAt,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
        HoldingView toHoldingView(BigDecimal totalMarketValue) {
            BigDecimal allocationPercent = marketValue == null || totalMarketValue.compareTo(BigDecimal.ZERO) == 0
                ? null
                : percentageOrNull(marketValue, totalMarketValue);
            return new HoldingView(
                id,
                instrumentId,
                ticker,
                exchangeCode,
                displayName,
                assetType,
                provider,
                providerQuoteId,
                currency,
                quantity,
                averagePurchasePrice,
                currentPrice,
                marketValue,
                costBasis,
                unrealizedGainLoss,
                unrealizedGainLossPercent,
                allocationPercent,
                priceStatus,
                priceObservedAt,
                createdAt,
                updatedAt
            );
        }
    }
}
