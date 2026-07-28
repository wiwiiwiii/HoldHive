package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.util.List;

import com.holdhive.portfolio.domain.AssetType;

class DemoPortfolioHoldingReader implements PortfolioHoldingReader {

    @Override
    public PortfolioSnapshot findDefaultPortfolio() {
        return new PortfolioSnapshot(
            1L,
            "My Portfolio",
            "USD",
            List.of(
                new HoldingPosition(
                    101L,
                    AssetType.STOCK,
                    "AAPL",
                    "105.AAPL",
                    new BigDecimal("10"),
                    new BigDecimal("175.50")
                ),
                new HoldingPosition(
                    102L,
                    AssetType.ETF,
                    "VOO",
                    "105.VOO",
                    new BigDecimal("2"),
                    new BigDecimal("460.00")
                ),
                new HoldingPosition(
                    103L,
                    AssetType.MUTUAL_FUND,
                    "FXAIX",
                    "MF:FXAIX",
                    new BigDecimal("12"),
                    new BigDecimal("180.00")
                ),
                new HoldingPosition(
                    104L,
                    AssetType.CRYPTO,
                    "BTC",
                    "CRYPTO:BTC",
                    new BigDecimal("0.05"),
                    new BigDecimal("58000.00")
                ),
                new HoldingPosition(
                    105L,
                    AssetType.CASH,
                    "USD",
                    "USD",
                    new BigDecimal("4500.00"),
                    BigDecimal.ONE
                ),
                new HoldingPosition(
                    106L,
                    AssetType.BANK_DEPOSIT,
                    "HSBC_USD",
                    "HSBC_USD",
                    new BigDecimal("3000.00"),
                    BigDecimal.ONE
                )
            )
        );
    }
}
