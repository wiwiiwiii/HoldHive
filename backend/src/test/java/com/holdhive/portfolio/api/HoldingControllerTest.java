package com.holdhive.portfolio.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HoldingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsListsGetsAndDeletesStockHolding() throws Exception {
        long holdingId = createHolding("""
            {
              "assetType": "STOCK",
              "ticker": " msft ",
              "exchangeCode": "nasdaq",
              "providerQuoteId": "105.MSFT",
              "quantity": 5,
              "averagePurchasePrice": 300
            }
            """);

        mockMvc.perform(get("/api/v1/holdings")
                .param("priceMode", "DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.items[0].id").value(holdingId))
            .andExpect(jsonPath("$.items[0].ticker").value("MSFT"))
            .andExpect(jsonPath("$.items[0].exchangeCode").value("NASDAQ"))
            .andExpect(jsonPath("$.items[0].assetType").value("STOCK"))
            .andExpect(jsonPath("$.items[0].provider").value("EASTMONEY"))
            .andExpect(jsonPath("$.items[0].providerQuoteId").value("105.MSFT"))
            .andExpect(jsonPath("$.items[0].quantity").value(5.00000000))
            .andExpect(jsonPath("$.items[0].averagePurchasePrice").value(300.00000000))
            .andExpect(jsonPath("$.items[0].currentPrice").value(330.00000000))
            .andExpect(jsonPath("$.items[0].marketValue").value(1650.00000000))
            .andExpect(jsonPath("$.items[0].costBasis").value(1500.00000000))
            .andExpect(jsonPath("$.items[0].unrealizedGainLoss").value(150.00000000))
            .andExpect(jsonPath("$.items[0].unrealizedGainLossPercent").value(10.00000000))
            .andExpect(jsonPath("$.items[0].allocationPercent").value(100.00000000))
            .andExpect(jsonPath("$.items[0].priceStatus").value("DEMO"))
            .andExpect(jsonPath("$.items[0].priceObservedAt").value("2026-07-24T08:29:00Z"));

        mockMvc.perform(get("/api/v1/holdings/{holdingId}", holdingId)
                .param("priceMode", "DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(holdingId))
            .andExpect(jsonPath("$.ticker").value("MSFT"));

        mockMvc.perform(delete("/api/v1/holdings/{holdingId}", holdingId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/holdings/{holdingId}", holdingId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOLDING_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void storesFixedValueAssetsWithFixedPriceConventions() throws Exception {
        long holdingId = createHolding("""
            {
              "assetType": "CASH",
              "ticker": "usd",
              "quantity": 4500,
              "averagePurchasePrice": 77
            }
            """);

        mockMvc.perform(get("/api/v1/holdings/{holdingId}", holdingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value("USD"))
            .andExpect(jsonPath("$.exchangeCode").value("CASH"))
            .andExpect(jsonPath("$.provider").value("FIXED"))
            .andExpect(jsonPath("$.averagePurchasePrice").value(1.00000000))
            .andExpect(jsonPath("$.currentPrice").value(1.00000000))
            .andExpect(jsonPath("$.marketValue").value(4500.00000000))
            .andExpect(jsonPath("$.priceStatus").value("FIXED"));
    }

    @Test
    void rejectsInvalidAndDuplicateHoldingRequests() throws Exception {
        mockMvc.perform(post("/api/v1/holdings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assetType": "STOCK",
                      "ticker": "BADQTY",
                      "quantity": 0,
                      "averagePurchasePrice": 100
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));

        createHolding("""
            {
              "assetType": "ETF",
              "ticker": "voo",
              "exchangeCode": "NYSE",
              "providerQuoteId": "105.VOO",
              "quantity": 2,
              "averagePurchasePrice": 460
            }
            """);

        mockMvc.perform(post("/api/v1/holdings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assetType": "ETF",
                      "ticker": "VOO",
                      "exchangeCode": "NYSE",
                      "providerQuoteId": "105.VOO",
                      "quantity": 1,
                      "averagePurchasePrice": 450
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOLDING_ALREADY_EXISTS"));
    }

    @Test
    void returnsEmptyHoldingListForEmptyPortfolio() throws Exception {
        mockMvc.perform(get("/api/v1/holdings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void sortsByMarketValueDescendingWithUnpricedHoldingsLast() throws Exception {
        createHolding("""
            {
              "assetType": "STOCK",
              "ticker": "UNKNOWN",
              "quantity": 1,
              "averagePurchasePrice": 10
            }
            """);
        createHolding("""
            {
              "assetType": "CASH",
              "ticker": "USD",
              "quantity": 100,
              "averagePurchasePrice": 1
            }
            """);
        createHolding("""
            {
              "assetType": "BANK_DEPOSIT",
              "ticker": "USD_DEPOSIT",
              "quantity": 250,
              "averagePurchasePrice": 1
            }
            """);

        mockMvc.perform(get("/api/v1/holdings")
                .param("sort", "marketValue,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].ticker").value("USD_DEPOSIT"))
            .andExpect(jsonPath("$.items[1].ticker").value("USD"))
            .andExpect(jsonPath("$.items[2].ticker").value("UNKNOWN"))
            .andExpect(jsonPath("$.items[2].marketValue").doesNotExist());
    }

    private long createHolding(String requestBody) throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/holdings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", notNullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        return response.get("id").asLong();
    }
}
