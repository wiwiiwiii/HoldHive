package com.holdhive.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.holdhive.pricing.domain.PriceStatus;

public record HoldingValuationInput(
    Long holdingId,
    AssetType assetType,
    String ticker,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice,
    BigDecimal currentPrice,
    PriceStatus priceStatus,
    Instant priceObservedAt
) {
    public HoldingValuationInput {
        assetType = assetType == null ? AssetType.STOCK : assetType;
    }
}
