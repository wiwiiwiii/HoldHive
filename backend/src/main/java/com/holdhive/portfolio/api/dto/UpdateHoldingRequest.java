package com.holdhive.portfolio.api.dto;

import java.math.BigDecimal;

import com.holdhive.portfolio.application.UpdateHoldingCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record UpdateHoldingRequest(
    @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 16, fraction = 8) BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.0") @Digits(integer = 16, fraction = 8) BigDecimal averagePurchasePrice
) {

    public UpdateHoldingCommand toCommand(Long holdingId) {
        return new UpdateHoldingCommand(
            holdingId,
            quantity,
            averagePurchasePrice
        );
    }
}
