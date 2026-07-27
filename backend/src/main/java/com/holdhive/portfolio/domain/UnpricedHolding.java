package com.holdhive.portfolio.domain;

public record UnpricedHolding(
    Long holdingId,
    AssetType assetType,
    String ticker,
    String reason
) {
}
