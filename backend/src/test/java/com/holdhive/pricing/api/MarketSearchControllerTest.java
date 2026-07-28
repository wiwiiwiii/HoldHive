package com.holdhive.pricing.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.MarketSearchItem;
import com.holdhive.pricing.application.MarketSearchQueryService;
import com.holdhive.pricing.application.MarketSearchResult;

@WebMvcTest(MarketSearchController.class)
class MarketSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketSearchQueryService marketSearchQueryService;

    @Test
    void returnsSearchResultsForQueryAndOptionalMarket() throws Exception {
        when(marketSearchQueryService.search("AAPL", "US")).thenReturn(new MarketSearchResult(
            "AAPL",
            List.of(new MarketSearchItem(
                "AAPL",
                "Apple Inc.",
                "NASDAQ",
                "EASTMONEY",
                "105.AAPL",
                AssetType.STOCK
            )),
            "DEMO",
            false
        ));

        mockMvc.perform(get("/api/v1/market/search")
                .param("query", "AAPL")
                .param("market", "US"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query").value("AAPL"))
            .andExpect(jsonPath("$.source").value("DEMO"))
            .andExpect(jsonPath("$.cached").value(false))
            .andExpect(jsonPath("$.results[0].ticker").value("AAPL"))
            .andExpect(jsonPath("$.results[0].displayName").value("Apple Inc."))
            .andExpect(jsonPath("$.results[0].exchangeCode").value("NASDAQ"))
            .andExpect(jsonPath("$.results[0].provider").value("EASTMONEY"))
            .andExpect(jsonPath("$.results[0].providerQuoteId").value("105.AAPL"))
            .andExpect(jsonPath("$.results[0].assetType").value("STOCK"));

        verify(marketSearchQueryService).search("AAPL", "US");
    }

    @Test
    void returnsValidationErrorWhenQueryIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/market/search"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("query"));
    }
}
