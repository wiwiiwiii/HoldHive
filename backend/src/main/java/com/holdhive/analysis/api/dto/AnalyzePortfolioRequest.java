package com.holdhive.analysis.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.holdhive.analysis.domain.model.AssetType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Wire-format request for {@code POST /api/v1/portfolio/analysis}.
 * Holdings are supplied directly by the caller with pre-computed market
 * values - this demo does not fetch live prices (see README.md scope notes).
 */
public record AnalyzePortfolioRequest(
        @NotEmpty(message = "holdings must not be empty")
        List<@Valid HoldingInput> holdings,

        String baseCurrency
) {

    public String baseCurrencyOrDefault() {
        return (baseCurrency == null || baseCurrency.isBlank()) ? "CNY" : baseCurrency;
    }

    public record HoldingInput(
            @NotBlank(message = "ticker must not be blank")
            String ticker,

            @NotNull(message = "assetType is required")
            AssetType assetType,

            BigDecimal quantity,

            @NotNull(message = "marketValue is required")
            @DecimalMin(value = "0", message = "marketValue must not be negative")
            BigDecimal marketValue,

            BigDecimal costBasis
    ) {
    }
}
