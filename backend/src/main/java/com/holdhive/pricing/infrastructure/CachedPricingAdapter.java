package com.holdhive.pricing.infrastructure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.holdhive.portfolio.persistence.entity.InstrumentEntity;
import com.holdhive.portfolio.persistence.entity.PriceSnapshotEntity;
import com.holdhive.portfolio.persistence.repository.InstrumentRepository;
import com.holdhive.portfolio.persistence.repository.PriceSnapshotRepository;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

public class CachedPricingAdapter implements PricingAdapter {

    private static final int SCALE = 8;

    private final MarketQuoteProvider liveProvider;
    private final MarketQuoteProvider demoProvider;
    private final InstrumentRepository instrumentRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final MarketDataProperties properties;
    private final MarketClock clock;

    public CachedPricingAdapter(
        MarketQuoteProvider liveProvider,
        MarketQuoteProvider demoProvider,
        InstrumentRepository instrumentRepository,
        PriceSnapshotRepository priceSnapshotRepository,
        MarketDataProperties properties,
        MarketClock clock
    ) {
        this.liveProvider = Objects.requireNonNull(liveProvider, "liveProvider must not be null");
        this.demoProvider = Objects.requireNonNull(demoProvider, "demoProvider must not be null");
        this.instrumentRepository = Objects.requireNonNull(instrumentRepository, "instrumentRepository must not be null");
        this.priceSnapshotRepository = Objects.requireNonNull(
            priceSnapshotRepository,
            "priceSnapshotRepository must not be null"
        );
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public MarketQuote quote(String providerQuoteId) {
        String normalizedId = normalize(providerQuoteId);
        if (normalizedId.isBlank()) {
            return MarketQuote.unavailable("MIXED", "", "");
        }
        return quotes(List.of(normalizedId)).stream()
            .findFirst()
            .orElseGet(() -> MarketQuote.unavailable("MIXED", normalizedId, ticker(normalizedId)));
    }

    @Override
    public List<MarketQuote> quotes(List<String> providerQuoteIds) {
        List<String> requestedIds = providerQuoteIds.stream()
            .filter(Objects::nonNull)
            .map(CachedPricingAdapter::normalize)
            .filter(providerQuoteId -> !providerQuoteId.isBlank())
            .distinct()
            .toList();
        Map<String, MarketQuote> liveById = fetchLiveQuotes(requestedIds);
        Map<String, MarketQuote> demoById = fetchDemoQuotes(requestedIds);

        return requestedIds.stream()
            .map(providerQuoteId -> bestAvailable(providerQuoteId, liveById, demoById))
            .toList();
    }

    private MarketQuote bestAvailable(
        String providerQuoteId,
        Map<String, MarketQuote> liveById,
        Map<String, MarketQuote> demoById
    ) {
        MarketQuote liveQuote = liveById.get(providerQuoteId);
        if (isUsable(liveQuote)) {
            saveSnapshot(liveQuote, false);
            return liveQuote;
        }

        MarketQuote cachedQuote = cachedQuote(providerQuoteId);
        if (isUsable(cachedQuote)) {
            return cachedQuote;
        }

        MarketQuote demoQuote = demoById.get(providerQuoteId);
        if (isUsable(demoQuote)) {
            return demoQuote;
        }

        return MarketQuote.unavailable("MIXED", providerQuoteId, ticker(providerQuoteId));
    }

    private Map<String, MarketQuote> fetchLiveQuotes(List<String> providerQuoteIds) {
        if (!properties.isExternalEnabled() || providerQuoteIds.isEmpty()) {
            return Map.of();
        }
        try {
            return liveProvider.quotes(providerQuoteIds).stream()
                .collect(Collectors.toMap(
                    quote -> normalize(quote.providerQuoteId()),
                    Function.identity(),
                    (first, ignored) -> first
                ));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private Map<String, MarketQuote> fetchDemoQuotes(List<String> providerQuoteIds) {
        if (providerQuoteIds.isEmpty()) {
            return Map.of();
        }
        try {
            return demoProvider.quotes(providerQuoteIds).stream()
                .collect(Collectors.toMap(
                    quote -> normalize(quote.providerQuoteId()),
                    Function.identity(),
                    (first, ignored) -> first
                ));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private MarketQuote cachedQuote(String providerQuoteId) {
        return priceSnapshotRepository
            .findFirstByInstrument_ProviderQuoteIdIgnoreCaseOrderByObservedAtDesc(providerQuoteId)
            .filter(this::isFresh)
            .map(snapshot -> {
                InstrumentEntity instrument = snapshot.getInstrument();
                return new MarketQuote(
                    snapshot.getProvider(),
                    providerQuoteId,
                    instrument.getTicker(),
                    instrument.getDisplayName() == null ? instrument.getTicker() : instrument.getDisplayName(),
                    snapshot.getCurrency(),
                    scale(snapshot.getPrice()),
                    snapshot.isDemo() ? PriceStatus.DEMO : PriceStatus.CACHED,
                    snapshot.getObservedAt()
                );
            })
            .orElse(null);
    }

    private boolean isFresh(PriceSnapshotEntity snapshot) {
        Instant oldestAllowed = clock.now().minus(properties.getCacheTtl());
        return !snapshot.getObservedAt().isBefore(oldestAllowed);
    }

    private void saveSnapshot(MarketQuote quote, boolean demo) {
        if (quote == null || quote.currentPrice() == null || quote.priceStatus() != PriceStatus.LIVE) {
            return;
        }
        instrumentRepository.findFirstByProviderQuoteIdIgnoreCase(quote.providerQuoteId())
            .ifPresent(instrument -> priceSnapshotRepository.save(new PriceSnapshotEntity(
                instrument,
                scale(quote.currentPrice()),
                quote.currency(),
                quote.provider(),
                demo,
                quote.priceObservedAt() == null ? clock.now() : quote.priceObservedAt()
            )));
    }

    private static boolean isUsable(MarketQuote quote) {
        return quote != null
            && quote.currentPrice() != null
            && quote.priceStatus() != PriceStatus.UNAVAILABLE;
    }

    private static String ticker(String providerQuoteId) {
        String normalized = normalize(providerQuoteId);
        if (normalized.startsWith("CRYPTO:")) {
            return normalized.substring("CRYPTO:".length());
        }
        int dotIndex = normalized.indexOf('.');
        return dotIndex >= 0 && dotIndex + 1 < normalized.length()
            ? normalized.substring(dotIndex + 1)
            : normalized;
    }

    private static String normalize(String providerQuoteId) {
        return providerQuoteId == null ? "" : providerQuoteId.trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
