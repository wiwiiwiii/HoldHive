package com.holdhive.portfolio.application;

import java.math.BigDecimal;

import com.holdhive.portfolio.domain.AssetType;

public record HoldingPosition(
    Long holdingId,
    Long instrumentId,
    AssetType assetType,
    String ticker,
    String providerQuoteId,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice
) {
    public HoldingPosition(
        Long holdingId,
        AssetType assetType,
        String ticker,
        String providerQuoteId,
        BigDecimal quantity,
        BigDecimal averagePurchasePrice
    ) {
        this(holdingId, holdingId, assetType, ticker, providerQuoteId, quantity, averagePurchasePrice);
    }

    public HoldingPosition {
        instrumentId = instrumentId == null ? holdingId : instrumentId;
        assetType = assetType == null ? AssetType.STOCK : assetType;
    }
}
