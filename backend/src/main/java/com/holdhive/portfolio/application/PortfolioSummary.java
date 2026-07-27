package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.holdhive.portfolio.domain.PortfolioAllocation;
import com.holdhive.portfolio.domain.PortfolioValuation;
import com.holdhive.portfolio.domain.UnpricedHolding;
import com.holdhive.portfolio.domain.ValuationStatus;

public record PortfolioSummary(
    Long portfolioId,
    String portfolioName,
    String baseCurrency,
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

    public static PortfolioSummary from(PortfolioSnapshot snapshot, PortfolioValuation valuation) {
        return new PortfolioSummary(
            snapshot.portfolioId(),
            snapshot.portfolioName(),
            snapshot.baseCurrency(),
            valuation.holdingCount(),
            valuation.pricedHoldingCount(),
            valuation.valuationStatus(),
            valuation.totalCostBasis(),
            valuation.totalMarketValue(),
            valuation.totalUnrealizedGainLoss(),
            valuation.totalUnrealizedGainLossPercent(),
            valuation.priceAsOf(),
            valuation.allocations(),
            valuation.unpricedHoldings()
        );
    }
}
