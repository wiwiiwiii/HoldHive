package com.holdhive.portfolio.application;

import java.math.BigDecimal;

public record HoldingPosition(
    Long holdingId,
    String ticker,
    String providerQuoteId,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice
) {
}
