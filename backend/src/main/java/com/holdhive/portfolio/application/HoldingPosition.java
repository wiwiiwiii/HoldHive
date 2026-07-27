package com.holdhive.portfolio.application;

import java.math.BigDecimal;

import com.holdhive.portfolio.domain.AssetType;

public record HoldingPosition(
    Long holdingId,
    AssetType assetType,
    String ticker,
    String providerQuoteId,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice
) {
    public HoldingPosition {
        assetType = assetType == null ? AssetType.STOCK : assetType;
    }
}
