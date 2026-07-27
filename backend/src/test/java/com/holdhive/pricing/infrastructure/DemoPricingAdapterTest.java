package com.holdhive.pricing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

class DemoPricingAdapterTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-07-24T08:29:00Z");

    @Test
    void returnsDemoQuoteForConfiguredTicker() {
        DemoPricingAdapter adapter = new DemoPricingAdapter(
            Map.of("AAPL", new BigDecimal("210.25")),
            OBSERVED_AT
        );

        MarketQuote quote = adapter.quote("aapl");

        assertThat(quote.provider()).isEqualTo("DEMO");
        assertThat(quote.providerQuoteId()).isEqualTo("AAPL");
        assertThat(quote.ticker()).isEqualTo("AAPL");
        assertThat(quote.currentPrice()).isEqualByComparingTo("210.25000000");
        assertThat(quote.priceStatus()).isEqualTo(PriceStatus.DEMO);
        assertThat(quote.priceObservedAt()).isEqualTo(OBSERVED_AT);
    }

    @Test
    void returnsUnavailableQuoteForUnknownTicker() {
        DemoPricingAdapter adapter = new DemoPricingAdapter(
            Map.of("AAPL", new BigDecimal("210.25")),
            OBSERVED_AT
        );

        MarketQuote quote = adapter.quote("unknown");

        assertThat(quote.provider()).isEqualTo("DEMO");
        assertThat(quote.providerQuoteId()).isEqualTo("UNKNOWN");
        assertThat(quote.ticker()).isEqualTo("UNKNOWN");
        assertThat(quote.currentPrice()).isNull();
        assertThat(quote.priceStatus()).isEqualTo(PriceStatus.UNAVAILABLE);
        assertThat(quote.priceObservedAt()).isNull();
    }

    @Test
    void returnsBatchQuotesInRequestOrder() {
        DemoPricingAdapter adapter = new DemoPricingAdapter(
            Map.of(
                "AAPL", new BigDecimal("210.25"),
                "MSFT", new BigDecimal("330.00")
            ),
            OBSERVED_AT
        );

        List<MarketQuote> quotes = adapter.quotes(List.of("msft", "unknown", "aapl"));

        assertThat(quotes).extracting(MarketQuote::ticker)
            .containsExactly("MSFT", "UNKNOWN", "AAPL");
        assertThat(quotes).extracting(MarketQuote::priceStatus)
            .containsExactly(PriceStatus.DEMO, PriceStatus.UNAVAILABLE, PriceStatus.DEMO);
    }
}
