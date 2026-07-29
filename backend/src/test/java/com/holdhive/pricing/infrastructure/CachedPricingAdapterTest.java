package com.holdhive.pricing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.portfolio.persistence.entity.InstrumentEntity;
import com.holdhive.portfolio.persistence.entity.PriceSnapshotEntity;
import com.holdhive.portfolio.persistence.repository.InstrumentRepository;
import com.holdhive.portfolio.persistence.repository.PriceSnapshotRepository;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

class CachedPricingAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-29T00:00:00Z");

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceSnapshotRepository priceSnapshotRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void usesLiveProviderBeforeCacheOrDemo() {
        CachedPricingAdapter adapter = adapter(
            ids -> List.of(liveQuote("105.AAPL", "AAPL", "210.25")),
            ids -> List.of()
        );

        MarketQuote quote = adapter.quote("105.AAPL");

        assertThat(quote.priceStatus()).isEqualTo(PriceStatus.LIVE);
        assertThat(quote.currentPrice()).isEqualByComparingTo("210.25000000");
    }

    @Test
    void returnsLiveQuoteWhenSnapshotWriteFails() {
        InstrumentEntity instrument = new InstrumentEntity(
            AssetType.STOCK,
            "AAPL",
            "NASDAQ",
            "Apple Inc.",
            "EASTMONEY",
            "105.AAPL",
            "USD"
        );
        when(instrumentRepository.findFirstByProviderQuoteIdIgnoreCase("105.AAPL"))
            .thenReturn(Optional.of(instrument));
        when(priceSnapshotRepository.save(any(PriceSnapshotEntity.class)))
            .thenThrow(new InvalidDataAccessApiUsageException("Connection is read-only"));

        CachedPricingAdapter adapter = adapter(
            ids -> List.of(liveQuote("105.AAPL", "AAPL", "210.25")),
            ids -> List.of()
        );

        MarketQuote quote = adapter.quote("105.AAPL");

        assertThat(quote.priceStatus()).isEqualTo(PriceStatus.LIVE);
        assertThat(quote.currentPrice()).isEqualByComparingTo("210.25000000");
    }

    @Test
    void skipsSnapshotWriteInsideReadOnlyTransaction() {
        InstrumentEntity instrument = new InstrumentEntity(
            AssetType.STOCK,
            "AAPL",
            "NASDAQ",
            "Apple Inc.",
            "EASTMONEY",
            "105.AAPL",
            "USD"
        );
        when(instrumentRepository.findFirstByProviderQuoteIdIgnoreCase("105.AAPL"))
            .thenReturn(Optional.of(instrument));
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            CachedPricingAdapter adapter = adapter(
                ids -> List.of(liveQuote("105.AAPL", "AAPL", "210.25")),
                ids -> List.of()
            );

            MarketQuote quote = adapter.quote("105.AAPL");

            assertThat(quote.priceStatus()).isEqualTo(PriceStatus.LIVE);
            assertThat(quote.currentPrice()).isEqualByComparingTo("210.25000000");
            verify(priceSnapshotRepository, never()).save(any(PriceSnapshotEntity.class));
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        }
    }

    @Test
    void fallsBackToFreshDatabaseSnapshotWhenLiveProviderIsUnavailable() {
        InstrumentEntity instrument = new InstrumentEntity(
            AssetType.STOCK,
            "AAPL",
            "NASDAQ",
            "Apple Inc.",
            "EASTMONEY",
            "105.AAPL",
            "USD"
        );
        PriceSnapshotEntity snapshot = new PriceSnapshotEntity(
            instrument,
            new BigDecimal("205.12"),
            "USD",
            "EASTMONEY",
            false,
            NOW.minusSeconds(30)
        );
        when(priceSnapshotRepository.findFirstByInstrument_ProviderQuoteIdIgnoreCaseOrderByObservedAtDesc("105.AAPL"))
            .thenReturn(Optional.of(snapshot));

        CachedPricingAdapter adapter = adapter(ids -> List.of(), ids -> List.of());

        MarketQuote quote = adapter.quote("105.AAPL");

        assertThat(quote.priceStatus()).isEqualTo(PriceStatus.CACHED);
        assertThat(quote.currentPrice()).isEqualByComparingTo("205.12000000");
        assertThat(quote.priceObservedAt()).isEqualTo(NOW.minusSeconds(30));
    }

    @Test
    void fallsBackToDemoWhenNoLiveOrFreshCacheExists() {
        CachedPricingAdapter adapter = adapter(
            ids -> List.of(),
            ids -> List.of(MarketQuote.demo("105.AAPL", "AAPL", new BigDecimal("200.00"), NOW.minusSeconds(300)))
        );

        MarketQuote quote = adapter.quote("105.AAPL");

        assertThat(quote.priceStatus()).isEqualTo(PriceStatus.DEMO);
        assertThat(quote.currentPrice()).isEqualByComparingTo("200.00000000");
    }

    private CachedPricingAdapter adapter(
        MarketQuoteProvider liveProvider,
        MarketQuoteProvider demoProvider
    ) {
        return new CachedPricingAdapter(
            liveProvider,
            demoProvider,
            instrumentRepository,
            priceSnapshotRepository,
            new MarketDataProperties(true, Duration.ofSeconds(60), Duration.ofSeconds(2)),
            () -> NOW
        );
    }

    private static MarketQuote liveQuote(String providerQuoteId, String ticker, String price) {
        return new MarketQuote(
            "EASTMONEY",
            providerQuoteId,
            ticker,
            ticker,
            "USD",
            new BigDecimal(price),
            PriceStatus.LIVE,
            NOW
        );
    }
}
