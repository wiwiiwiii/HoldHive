package com.holdhive.portfolio.api;

import static org.hamcrest.Matchers.notNullValue;
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

import com.holdhive.portfolio.application.PortfolioSummary;
import com.holdhive.portfolio.application.PortfolioSummaryService;
import com.holdhive.portfolio.domain.PortfolioAllocation;
import com.holdhive.portfolio.domain.UnpricedHolding;
import com.holdhive.portfolio.domain.ValuationStatus;
import com.holdhive.pricing.application.PriceMode;

@WebMvcTest(PortfolioSummaryController.class)
class PortfolioSummaryControllerTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-07-24T08:29:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioSummaryService portfolioSummaryService;

    @Test
    void returnsPortfolioSummaryUsingDefaultPriceMode() throws Exception {
        when(portfolioSummaryService.getSummary(PriceMode.BEST_AVAILABLE)).thenReturn(summary());

        mockMvc.perform(get("/api/v1/portfolio/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portfolioId").value(1))
            .andExpect(jsonPath("$.portfolioName").value("My Portfolio"))
            .andExpect(jsonPath("$.baseCurrency").value("USD"))
            .andExpect(jsonPath("$.holdingCount").value(3))
            .andExpect(jsonPath("$.pricedHoldingCount").value(2))
            .andExpect(jsonPath("$.valuationStatus").value("PARTIAL"))
            .andExpect(jsonPath("$.totalCostBasis").value(4255.00000000))
            .andExpect(jsonPath("$.totalMarketValue").value(3752.50000000))
            .andExpect(jsonPath("$.totalUnrealizedGainLoss").value(497.50000000))
            .andExpect(jsonPath("$.totalUnrealizedGainLossPercent").value(11.69212691))
            .andExpect(jsonPath("$.priceAsOf").value("2026-07-24T08:29:00Z"))
            .andExpect(jsonPath("$.allocations[0].holdingId").value(101))
            .andExpect(jsonPath("$.allocations[0].ticker").value("AAPL"))
            .andExpect(jsonPath("$.allocations[0].marketValue").value(2102.50000000))
            .andExpect(jsonPath("$.allocations[0].allocationPercent").value(56.02931379))
            .andExpect(jsonPath("$.unpricedHoldings[0].holdingId").value(103))
            .andExpect(jsonPath("$.unpricedHoldings[0].ticker").value("UNKNOWN"))
            .andExpect(jsonPath("$.unpricedHoldings[0].reason").value("PRICE_UNAVAILABLE"));

        verify(portfolioSummaryService).getSummary(PriceMode.BEST_AVAILABLE);
    }

    @Test
    void acceptsExplicitDemoAllowedPriceMode() throws Exception {
        when(portfolioSummaryService.getSummary(PriceMode.DEMO_ALLOWED)).thenReturn(summary());

        mockMvc.perform(get("/api/v1/portfolio/summary?priceMode=DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp").doesNotExist())
            .andExpect(jsonPath("$.priceAsOf", notNullValue()));

        verify(portfolioSummaryService).getSummary(PriceMode.DEMO_ALLOWED);
    }

    private static PortfolioSummary summary() {
        return new PortfolioSummary(
            1L,
            "My Portfolio",
            "USD",
            3,
            2,
            ValuationStatus.PARTIAL,
            new BigDecimal("4255.00000000"),
            new BigDecimal("3752.50000000"),
            new BigDecimal("497.50000000"),
            new BigDecimal("11.69212691"),
            OBSERVED_AT,
            List.of(
                new PortfolioAllocation(
                    101L,
                    "AAPL",
                    new BigDecimal("2102.50000000"),
                    new BigDecimal("56.02931379")
                )
            ),
            List.of(new UnpricedHolding(103L, "UNKNOWN", "PRICE_UNAVAILABLE"))
        );
    }
}
