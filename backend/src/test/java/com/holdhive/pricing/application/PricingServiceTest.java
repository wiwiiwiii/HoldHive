package com.holdhive.pricing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.infrastructure.PricingAdapter;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingAdapter pricingAdapter;

    @Test
    void delegatesSingleQuoteLookupToConfiguredAdapter() {
        MarketQuote demoQuote = MarketQuote.demo(
            "AAPL",
            "AAPL",
            new BigDecimal("210.25000000"),
            Instant.parse("2026-07-24T08:29:00Z")
        );
        when(pricingAdapter.quote("AAPL")).thenReturn(demoQuote);

        PricingService service = new PricingService(pricingAdapter);

        MarketQuote quote = service.getQuote("AAPL");

        assertThat(quote).isEqualTo(demoQuote);
        verify(pricingAdapter).quote("AAPL");
    }

    @Test
    void delegatesBatchQuoteLookupToConfiguredAdapter() {
        List<String> providerQuoteIds = List.of("AAPL", "MSFT");
        List<MarketQuote> expectedQuotes = List.of(
            MarketQuote.demo("AAPL", "AAPL", new BigDecimal("210.25000000"), Instant.parse("2026-07-24T08:29:00Z")),
            MarketQuote.demo("MSFT", "MSFT", new BigDecimal("330.00000000"), Instant.parse("2026-07-24T08:29:00Z"))
        );
        when(pricingAdapter.quotes(providerQuoteIds)).thenReturn(expectedQuotes);

        PricingService service = new PricingService(pricingAdapter);

        List<MarketQuote> quotes = service.getQuotes(providerQuoteIds);

        assertThat(quotes).isEqualTo(expectedQuotes);
        verify(pricingAdapter).quotes(providerQuoteIds);
    }
}
