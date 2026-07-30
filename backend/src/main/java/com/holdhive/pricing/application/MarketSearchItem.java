package com.holdhive.pricing.application;

import com.holdhive.portfolio.domain.AssetType;

public record MarketSearchItem(
    String ticker,
    String displayName,
    String exchangeCode,
    String provider,
    String providerQuoteId,
    AssetType assetType
) {
}
