package com.holdhive.portfolio.application;

import java.util.List;

class EmptyPortfolioHoldingReader implements PortfolioHoldingReader {

    @Override
    public PortfolioSnapshot findDefaultPortfolio() {
        return new PortfolioSnapshot(1L, "My Portfolio", "USD", List.of());
    }
}
