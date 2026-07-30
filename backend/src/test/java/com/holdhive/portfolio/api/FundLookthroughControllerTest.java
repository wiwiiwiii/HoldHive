package com.holdhive.portfolio.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.holdhive.common.error.ApiException;
import com.holdhive.portfolio.application.FundComponent;
import com.holdhive.portfolio.application.FundLookthrough;
import com.holdhive.portfolio.application.FundLookthroughService;
import com.holdhive.portfolio.domain.AssetType;

@WebMvcTest(FundLookthroughController.class)
class FundLookthroughControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FundLookthroughService fundLookthroughService;

    @Test
    void returnsFundLookthroughPayload() throws Exception {
        when(fundLookthroughService.getLookthrough(102L)).thenReturn(new FundLookthrough(
            102L,
            "VOO",
            "Vanguard S&P 500 ETF",
            AssetType.ETF,
            LocalDate.parse("2026-06-30"),
            "DEMO_DISCLOSURE",
            new BigDecimal("41.15000000"),
            List.of(new FundComponent("AAPL", "Apple Inc.", AssetType.STOCK, new BigDecimal("7.12000000"))),
            List.of("Fund holdings are based on the latest available disclosure.")
        ));

        mockMvc.perform(get("/api/v1/funds/102/lookthrough"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fundInstrumentId").value(102))
            .andExpect(jsonPath("$.ticker").value("VOO"))
            .andExpect(jsonPath("$.assetType").value("ETF"))
            .andExpect(jsonPath("$.asOfDate").value("2026-06-30"))
            .andExpect(jsonPath("$.holdings[0].ticker").value("AAPL"))
            .andExpect(jsonPath("$.holdings[0].assetType").value("STOCK"))
            .andExpect(jsonPath("$.warnings[0]").value("Fund holdings are based on the latest available disclosure."));
    }

    @Test
    void returnsStandardErrorWhenLookthroughIsUnavailable() throws Exception {
        when(fundLookthroughService.getLookthrough(999L)).thenThrow(
            new ApiException(HttpStatus.NOT_FOUND, "FUND_LOOKTHROUGH_NOT_FOUND", "Fund lookthrough not found")
        );

        mockMvc.perform(get("/api/v1/funds/999/lookthrough"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("FUND_LOOKTHROUGH_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Fund lookthrough not found"));
    }
}
