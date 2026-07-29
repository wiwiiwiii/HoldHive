package com.holdhive.portfolio.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
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
import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.portfolio.persistence.entity.InstrumentEntity;
import com.holdhive.portfolio.persistence.repository.HoldingRepository;
import com.holdhive.portfolio.persistence.repository.InstrumentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HoldingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @BeforeEach
    void clearSeedHoldingsForControllerIsolation() {
        holdingRepository.deleteAll();
        holdingRepository.flush();
    }

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
            .andExpect(jsonPath("$.items[0].priceStatus").value("CACHED"))
            .andExpect(jsonPath("$.items[0].priceObservedAt", notNullValue()));

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
    void exposesInstrumentIdForFundLookthroughNavigation() throws Exception {
        long holdingId = createHolding("""
            {
              "assetType": "ETF",
              "ticker": "voo",
              "exchangeCode": "NYSE",
              "providerQuoteId": "105.VOO",
              "quantity": 2,
              "averagePurchasePrice": 460
            }
            """);

        String responseBody = mockMvc.perform(get("/api/v1/holdings/{holdingId}", holdingId)
                .param("priceMode", "DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(holdingId))
            .andExpect(jsonPath("$.instrumentId", notNullValue()))
            .andExpect(jsonPath("$.assetType").value("ETF"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long instrumentId = objectMapper.readTree(responseBody).get("instrumentId").asLong();

        mockMvc.perform(get("/api/v1/funds/{instrumentId}/lookthrough", instrumentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fundInstrumentId").value(instrumentId))
            .andExpect(jsonPath("$.ticker").value("VOO"));

        mockMvc.perform(get("/api/v1/holdings")
                .param("priceMode", "DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(holdingId))
            .andExpect(jsonPath("$.items[0].instrumentId").value(instrumentId));
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
    void refreshesQuoteMetadataWhenCreatingHoldingForExistingInstrument() throws Exception {
        instrumentRepository.saveAndFlush(new InstrumentEntity(
            AssetType.STOCK,
            "REFRESH1",
            "NASDAQ",
            "Refresh Metadata Test",
            null,
            null,
            "USD"
        ));

        long holdingId = createHolding("""
            {
              "assetType": "STOCK",
              "ticker": "refresh1",
              "exchangeCode": "nasdaq",
              "providerQuoteId": "105.MSFT",
              "quantity": 5,
              "averagePurchasePrice": 300
            }
            """);

        mockMvc.perform(get("/api/v1/holdings/{holdingId}", holdingId)
                .param("priceMode", "DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("EASTMONEY"))
            .andExpect(jsonPath("$.providerQuoteId").value("105.MSFT"))
            .andExpect(jsonPath("$.currentPrice").value(330.00000000))
            .andExpect(jsonPath("$.priceStatus").value("CACHED"));
    }

    @Test
    void updatesHoldingQuantityAndAveragePurchasePrice() throws Exception {
        long holdingId = createHolding("""
            {
              "assetType": "STOCK",
              "ticker": "msft",
              "exchangeCode": "nasdaq",
              "providerQuoteId": "105.MSFT",
              "quantity": 5,
              "averagePurchasePrice": 300
            }
            """);

        mockMvc.perform(patch("/api/v1/holdings/{holdingId}", holdingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "quantity": 8,
                      "averagePurchasePrice": 320
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(holdingId))
            .andExpect(jsonPath("$.quantity").value(8.00000000))
            .andExpect(jsonPath("$.averagePurchasePrice").value(320.00000000))
            .andExpect(jsonPath("$.costBasis").value(2560.00000000));

        mockMvc.perform(get("/api/v1/holdings/{holdingId}", holdingId)
                .param("priceMode", "DEMO_ALLOWED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(8.00000000))
            .andExpect(jsonPath("$.averagePurchasePrice").value(320.00000000))
            .andExpect(jsonPath("$.marketValue").value(2640.00000000));
    }

    @Test
    void updatesHoldingUsingRequestedPriceModeForResponseValuation() throws Exception {
        long holdingId = createHolding("""
            {
              "assetType": "STOCK",
              "ticker": "aapl",
              "exchangeCode": "nasdaq",
              "providerQuoteId": "105.AAPL",
              "quantity": 2,
              "averagePurchasePrice": 175
            }
            """);

        mockMvc.perform(patch("/api/v1/holdings/{holdingId}", holdingId)
                .param("priceMode", "DEMO_ALLOWED")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "quantity": 3,
                      "averagePurchasePrice": 180
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(holdingId))
            .andExpect(jsonPath("$.quantity").value(3.00000000))
            .andExpect(jsonPath("$.averagePurchasePrice").value(180.00000000))
            .andExpect(jsonPath("$.currentPrice").value(210.25000000))
            .andExpect(jsonPath("$.marketValue").value(630.75000000))
            .andExpect(jsonPath("$.priceStatus").value("CACHED"));
    }

    @Test
    void keepsFixedValueAssetAveragePurchasePriceAtOneWhenUpdating() throws Exception {
        long holdingId = createHolding("""
            {
              "assetType": "BANK_DEPOSIT",
              "ticker": "usd_deposit",
              "quantity": 3000,
              "averagePurchasePrice": 99
            }
            """);

        mockMvc.perform(patch("/api/v1/holdings/{holdingId}", holdingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "quantity": 3500,
                      "averagePurchasePrice": 88
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value("USD_DEPOSIT"))
            .andExpect(jsonPath("$.quantity").value(3500.00000000))
            .andExpect(jsonPath("$.averagePurchasePrice").value(1.00000000))
            .andExpect(jsonPath("$.currentPrice").value(1.00000000))
            .andExpect(jsonPath("$.marketValue").value(3500.00000000))
            .andExpect(jsonPath("$.priceStatus").value("FIXED"));
    }

    @Test
    void rejectsInvalidHoldingUpdateRequests() throws Exception {
        long holdingId = createHolding("""
            {
              "assetType": "STOCK",
              "ticker": "aapl",
              "exchangeCode": "nasdaq",
              "providerQuoteId": "105.AAPL",
              "quantity": 2,
              "averagePurchasePrice": 175
            }
            """);

        mockMvc.perform(patch("/api/v1/holdings/{holdingId}", holdingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "quantity": -1,
                      "averagePurchasePrice": 175
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));

        mockMvc.perform(patch("/api/v1/holdings/{holdingId}", 999999)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "quantity": 1,
                      "averagePurchasePrice": 1
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOLDING_NOT_FOUND"));
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
