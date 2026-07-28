package com.holdhive.portfolio.application;

import java.math.BigDecimal;

public record UpdateHoldingCommand(
    Long holdingId,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice
) {
}
