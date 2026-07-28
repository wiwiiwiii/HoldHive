package com.holdhive.portfolio.api.dto;

import java.math.BigDecimal;

import com.holdhive.portfolio.application.CreateHoldingCommand;
import com.holdhive.portfolio.domain.AssetType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHoldingRequest(
    @NotNull AssetType assetType,
    @NotBlank @Size(max = 32) String ticker,
    @Size(max = 16) String exchangeCode,
    @Size(max = 160) String displayName,
    @Size(max = 64) String providerQuoteId,
    @Size(max = 3) String currency,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 16, fraction = 8) BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.0") @Digits(integer = 16, fraction = 8) BigDecimal averagePurchasePrice
) {

    public CreateHoldingCommand toCommand() {
        return new CreateHoldingCommand(
            assetType,
            ticker,
            exchangeCode,
            displayName,
            providerQuoteId,
            currency,
            quantity,
            averagePurchasePrice
        );
    }
}
