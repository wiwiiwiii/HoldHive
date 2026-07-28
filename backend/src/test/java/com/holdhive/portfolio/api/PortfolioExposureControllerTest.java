package com.holdhive.portfolio.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.holdhive.portfolio.application.PortfolioExposure;
import com.holdhive.portfolio.application.PortfolioExposureItem;
import com.holdhive.portfolio.application.PortfolioExposureService;
import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.PriceMode;

@WebMvcTest(PortfolioExposureController.class)
class PortfolioExposureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioExposureService portfolioExposureService;

    @Test
    void returnsPortfolioExposureUsingLookthroughFlagAndPriceMode() throws Exception {
        when(portfolioExposureService.getExposure(true, PriceMode.DEMO_ALLOWED)).thenReturn(new PortfolioExposure(
            1L,
            "My Portfolio",
            "USD",
            true,
            PriceMode.DEMO_ALLOWED,
            new BigDecimal("1000.00000000"),
            List.of(new PortfolioExposureItem(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                new BigDecimal("700.00000000"),
                new BigDecimal("50.00000000"),
                new BigDecimal("750.00000000"),
                new BigDecimal("75.00000000"),
                List.of("DIRECT", "FUND:VOO")
            )),
            List.of("AAPL appears both as direct holding and inside fund holdings.")
        ));

        mockMvc.perform(get("/api/v1/portfolio/exposure")
                .param("lookthrough", "true")
                .param("priceMode", "DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.portfolioId").value(1))
            .andExpect(jsonPath("$.lookthrough").value(true))
            .andExpect(jsonPath("$.priceMode").value("DEMO_ALLOWED"))
            .andExpect(jsonPath("$.items[0].ticker").value("AAPL"))
            .andExpect(jsonPath("$.items[0].directMarketValue").value(700.00000000))
            .andExpect(jsonPath("$.items[0].fundLookthroughMarketValue").value(50.00000000))
            .andExpect(jsonPath("$.warnings[0]").value("AAPL appears both as direct holding and inside fund holdings."));

        verify(portfolioExposureService).getExposure(true, PriceMode.DEMO_ALLOWED);
    }
}
