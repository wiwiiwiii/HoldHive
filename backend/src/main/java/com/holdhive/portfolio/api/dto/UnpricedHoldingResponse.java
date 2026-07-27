package com.holdhive.portfolio.api.dto;

import com.holdhive.portfolio.domain.UnpricedHolding;
import com.holdhive.portfolio.domain.AssetType;

public record UnpricedHoldingResponse(
    Long holdingId,
    AssetType assetType,
    String ticker,
    String reason
) {

    public static UnpricedHoldingResponse from(UnpricedHolding unpricedHolding) {
        return new UnpricedHoldingResponse(
            unpricedHolding.holdingId(),
            unpricedHolding.assetType(),
            unpricedHolding.ticker(),
            unpricedHolding.reason()
        );
    }
}
