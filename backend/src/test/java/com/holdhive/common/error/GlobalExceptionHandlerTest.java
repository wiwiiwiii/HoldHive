package com.holdhive.common.error;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.PriceMode;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new SampleController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
    }

    @Test
    void returnsFieldErrorsForValidationFailures() throws Exception {
        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "quantity": 0
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"))
            .andExpect(jsonPath("$.fieldErrors[0].message", notNullValue()))
            .andExpect(jsonPath("$.traceId", notNullValue()))
            .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returnsMalformedJsonForUnreadableRequestBody() throws Exception {
        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
            .andExpect(jsonPath("$.message").value("Malformed JSON request"))
            .andExpect(jsonPath("$.fieldErrors").isEmpty())
            .andExpect(jsonPath("$.traceId", notNullValue()))
            .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void returnsFieldErrorsForMissingRequestParameter() throws Exception {
        mockMvc.perform(get("/test/required-param"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("providerQuoteIds"))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void returnsFieldErrorsForInvalidRequestParameterEnum() throws Exception {
        mockMvc.perform(get("/test/price-mode")
                .param("priceMode", "NOT_A_MODE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("priceMode"))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void returnsFieldErrorsForInvalidRequestBodyEnum() throws Exception {
        mockMvc.perform(post("/test/asset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assetType": "NOT_A_TYPE"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("assetType"))
            .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void returnsConfiguredErrorCodeForApiException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOLDING_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Holding not found"))
            .andExpect(jsonPath("$.fieldErrors").isEmpty())
            .andExpect(jsonPath("$.traceId", notNullValue()))
            .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @RestController
    @RequestMapping("/test")
    public static class SampleController {

        @PostMapping("/validate")
        void validate(@Valid @RequestBody SampleRequest request) {
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ApiException(HttpStatus.NOT_FOUND, "HOLDING_NOT_FOUND", "Holding not found");
        }

        @GetMapping("/required-param")
        void requiredParam(@RequestParam String providerQuoteIds) {
        }

        @GetMapping("/price-mode")
        void priceMode(@RequestParam PriceMode priceMode) {
        }

        @PostMapping("/asset")
        void asset(@RequestBody AssetRequest request) {
        }
    }

    record SampleRequest(@Positive BigDecimal quantity) {
    }

    record AssetRequest(AssetType assetType) {
    }
}
