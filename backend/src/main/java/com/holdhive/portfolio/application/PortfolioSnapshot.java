package com.holdhive.portfolio.application;

import java.util.List;

public record PortfolioSnapshot(
    Long portfolioId,
    String portfolioName,
    String baseCurrency,
    List<HoldingPosition> holdings
) {

    public PortfolioSnapshot {
        holdings = List.copyOf(holdings);
    }
}
