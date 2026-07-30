package com.holdhive.portfolio.api.dto;

import java.math.BigDecimal;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.portfolio.domain.PortfolioAllocation;

public record AllocationResponse(
    Long holdingId,
    AssetType assetType,
    String ticker,
    BigDecimal marketValue,
    BigDecimal allocationPercent
) {

    public static AllocationResponse from(PortfolioAllocation allocation) {
        return new AllocationResponse(
            allocation.holdingId(),
            allocation.assetType(),
            allocation.ticker(),
            allocation.marketValue(),
            allocation.allocationPercent()
        );
    }
}
