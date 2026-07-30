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

    @Test
    void supportsPortfolioDemoQuoteIdsAcrossAssetTypes() {
        DemoPricingAdapter adapter = new DemoPricingAdapter(
            Map.ofEntries(
                Map.entry("105.NVDA", new BigDecimal("940.00")),
                Map.entry("105.QQQ", new BigDecimal("485.75")),
                Map.entry("FUND:005827", new BigDecimal("2.11")),
                Map.entry("CRYPTO:BTC", new BigDecimal("67500.00"))
            ),
            OBSERVED_AT
        );

        List<MarketQuote> quotes = adapter.quotes(List.of("105.NVDA", "105.QQQ", "FUND:005827", "CRYPTO:BTC"));

        assertThat(quotes).extracting(MarketQuote::currentPrice)
            .containsExactly(
                new BigDecimal("940.00000000"),
                new BigDecimal("485.75000000"),
                new BigDecimal("2.11000000"),
                new BigDecimal("67500.00000000")
            );
    }
}
