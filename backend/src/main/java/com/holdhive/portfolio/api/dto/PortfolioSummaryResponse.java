package com.holdhive.portfolio.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.holdhive.portfolio.application.PortfolioSummary;
import com.holdhive.portfolio.domain.ValuationStatus;

public record PortfolioSummaryResponse(
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
    List<AllocationResponse> allocations,
    List<UnpricedHoldingResponse> unpricedHoldings
) {

    public static PortfolioSummaryResponse from(PortfolioSummary summary) {
        return new PortfolioSummaryResponse(
            summary.portfolioId(),
            summary.portfolioName(),
            summary.baseCurrency(),
            summary.holdingCount(),
            summary.pricedHoldingCount(),
            summary.valuationStatus(),
            summary.totalCostBasis(),
            summary.totalMarketValue(),
            summary.totalUnrealizedGainLoss(),
            summary.totalUnrealizedGainLossPercent(),
            summary.priceAsOf(),
            summary.allocations().stream().map(AllocationResponse::from).toList(),
            summary.unpricedHoldings().stream().map(UnpricedHoldingResponse::from).toList()
        );
    }
}
