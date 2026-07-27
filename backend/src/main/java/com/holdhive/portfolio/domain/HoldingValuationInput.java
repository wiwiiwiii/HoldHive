package com.holdhive.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.holdhive.pricing.domain.PriceStatus;

public record HoldingValuationInput(
    Long holdingId,
    String ticker,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice,
    BigDecimal currentPrice,
    PriceStatus priceStatus,
    Instant priceObservedAt
) {
}
