package com.holdhive.portfolio.api.dto;

import com.holdhive.portfolio.domain.UnpricedHolding;

public record UnpricedHoldingResponse(
    Long holdingId,
    String ticker,
    String reason
) {

    public static UnpricedHoldingResponse from(UnpricedHolding unpricedHolding) {
        return new UnpricedHoldingResponse(
            unpricedHolding.holdingId(),
            unpricedHolding.ticker(),
            unpricedHolding.reason()
        );
    }
}
