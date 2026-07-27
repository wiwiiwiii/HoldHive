package com.holdhive.pricing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

@ExtendWith(MockitoExtension.class)
class MarketQuoteQueryServiceTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-07-24T08:29:00Z");

    @Mock
    private PricingService pricingService;

    private MarketQuoteQueryService service;

    @BeforeEach
    void setUp() {
        service = new MarketQuoteQueryService(pricingService);
    }

    @Test
    void returnsAcceptedQuotesAndUnavailableItemsInRequestOrder() {
        when(pricingService.getQuotes(List.of("105.AAPL", "UNKNOWN", "105.MSFT"))).thenReturn(List.of(
            quote("105.AAPL", "AAPL", PriceStatus.LIVE, "210.25"),
            MarketQuote.unavailable("DEMO", "UNKNOWN", "UNKNOWN"),
            quote("105.MSFT", "MSFT", PriceStatus.DEMO, "330.00")
        ));

        MarketQuoteResult result = service.getQuotes(
            List.of("105.AAPL", " ", "UNKNOWN", "105.MSFT"),
            PriceMode.DEMO_ALLOWED
        );

        assertThat(result.provider()).isEqualTo("MIXED");
        assertThat(result.priceMode()).isEqualTo(PriceMode.DEMO_ALLOWED);
        assertThat(result.quotes()).extracting(MarketQuote::providerQuoteId)
            .containsExactly("105.AAPL", "105.MSFT");
        assertThat(result.unavailable()).singleElement()
            .satisfies(unavailable -> {
                assertThat(unavailable.providerQuoteId()).isEqualTo("UNKNOWN");
                assertThat(unavailable.reason()).isEqualTo("PRICE_UNAVAILABLE");
            });
        verify(pricingService).getQuotes(List.of("105.AAPL", "UNKNOWN", "105.MSFT"));
    }

    @Test
    void liveOnlyRejectsCachedAndDemoQuotes() {
        when(pricingService.getQuotes(List.of("105.AAPL", "105.MSFT", "105.TSLA"))).thenReturn(List.of(
            quote("105.AAPL", "AAPL", PriceStatus.LIVE, "210.25"),
            quote("105.MSFT", "MSFT", PriceStatus.CACHED, "330.00"),
            quote("105.TSLA", "TSLA", PriceStatus.DEMO, "260.00")
        ));

        MarketQuoteResult result = service.getQuotes(
            List.of("105.AAPL", "105.MSFT", "105.TSLA"),
            PriceMode.LIVE_ONLY
        );

        assertThat(result.quotes()).extracting(MarketQuote::providerQuoteId)
            .containsExactly("105.AAPL");
        assertThat(result.unavailable()).extracting(UnavailableQuote::providerQuoteId)
            .containsExactly("105.MSFT", "105.TSLA");
        assertThat(result.unavailable()).extracting(UnavailableQuote::reason)
            .containsExactly("PRICE_MODE_REJECTED", "PRICE_MODE_REJECTED");
    }

    private static MarketQuote quote(
        String providerQuoteId,
        String ticker,
        PriceStatus priceStatus,
        String currentPrice
    ) {
        return new MarketQuote(
            "DEMO",
            providerQuoteId,
            ticker,
            ticker,
            "USD",
            new BigDecimal(currentPrice),
            priceStatus,
            OBSERVED_AT
        );
    }
}
