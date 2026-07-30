package com.holdhive.pricing.api.dto;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.MarketSearchItem;

public record MarketSearchItemResponse(
    String ticker,
    String displayName,
    String exchangeCode,
    String provider,
    String providerQuoteId,
    AssetType assetType
) {

    public static MarketSearchItemResponse from(MarketSearchItem item) {
        return new MarketSearchItemResponse(
            item.ticker(),
            item.displayName(),
            item.exchangeCode(),
            item.provider(),
            item.providerQuoteId(),
            item.assetType()
        );
    }
}
