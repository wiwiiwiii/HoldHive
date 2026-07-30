package com.holdhive.portfolio.domain;

import java.math.BigDecimal;

public record PortfolioAllocation(
    Long holdingId,
    AssetType assetType,
    String ticker,
    BigDecimal marketValue,
    BigDecimal allocationPercent
) {
}
