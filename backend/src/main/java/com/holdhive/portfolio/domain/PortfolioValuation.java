package com.holdhive.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PortfolioValuation(
    int holdingCount,
    int pricedHoldingCount,
    ValuationStatus valuationStatus,
    BigDecimal totalCostBasis,
    BigDecimal totalMarketValue,
    BigDecimal totalUnrealizedGainLoss,
    BigDecimal totalUnrealizedGainLossPercent,
    Instant priceAsOf,
    List<PortfolioAllocation> allocations,
    List<UnpricedHolding> unpricedHoldings
) {
}
