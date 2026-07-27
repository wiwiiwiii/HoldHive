package com.holdhive.pricing.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.holdhive.pricing.application.MarketQuoteQueryService;
import com.holdhive.pricing.application.MarketQuoteResult;
import com.holdhive.pricing.application.PriceMode;
import com.holdhive.pricing.application.UnavailableQuote;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

@WebMvcTest(MarketQuoteController.class)
class MarketQuoteControllerTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-07-24T08:29:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketQuoteQueryService marketQuoteQueryService;

    @Test
    void returnsMarketQuotesForCommaSeparatedProviderQuoteIds() throws Exception {
        when(marketQuoteQueryService.getQuotes(
            List.of("105.AAPL", "UNKNOWN"),
            PriceMode.DEMO_ALLOWED
        )).thenReturn(new MarketQuoteResult(
            "DEMO",
            PriceMode.DEMO_ALLOWED,
            List.of(new MarketQuote(
                "DEMO",
                "105.AAPL",
                "AAPL",
                "Apple Inc.",
                "USD",
                new BigDecimal("210.25000000"),
                PriceStatus.DEMO,
                OBSERVED_AT
            )),
            List.of(new UnavailableQuote("UNKNOWN", "PRICE_UNAVAILABLE"))
        ));

        mockMvc.perform(get("/api/v1/market/quotes")
                .param("providerQuoteIds", "105.AAPL,UNKNOWN")
                .param("priceMode", "DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("DEMO"))
            .andExpect(jsonPath("$.priceMode").value("DEMO_ALLOWED"))
            .andExpect(jsonPath("$.quotes[0].provider").value("DEMO"))
            .andExpect(jsonPath("$.quotes[0].providerQuoteId").value("105.AAPL"))
            .andExpect(jsonPath("$.quotes[0].ticker").value("AAPL"))
            .andExpect(jsonPath("$.quotes[0].displayName").value("Apple Inc."))
            .andExpect(jsonPath("$.quotes[0].currency").value("USD"))
            .andExpect(jsonPath("$.quotes[0].currentPrice").value(210.25000000))
            .andExpect(jsonPath("$.quotes[0].priceStatus").value("DEMO"))
            .andExpect(jsonPath("$.quotes[0].priceObservedAt").value("2026-07-24T08:29:00Z"))
            .andExpect(jsonPath("$.unavailable[0].providerQuoteId").value("UNKNOWN"))
            .andExpect(jsonPath("$.unavailable[0].reason").value("PRICE_UNAVAILABLE"));

        verify(marketQuoteQueryService).getQuotes(
            List.of("105.AAPL", "UNKNOWN"),
            PriceMode.DEMO_ALLOWED
        );
    }
}
