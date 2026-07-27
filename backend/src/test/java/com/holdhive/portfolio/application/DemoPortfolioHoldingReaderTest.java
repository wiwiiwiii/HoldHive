package com.holdhive.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.holdhive.portfolio.domain.AssetType;

class DemoPortfolioHoldingReaderTest {

    @Test
    void returnsStarterHoldingsAcrossSupportedAssetTypes() {
        PortfolioSnapshot snapshot = new DemoPortfolioHoldingReader().findDefaultPortfolio();

        assertThat(snapshot.portfolioName()).isEqualTo("My Portfolio");
        assertThat(snapshot.baseCurrency()).isEqualTo("USD");
        assertThat(snapshot.holdings()).extracting(HoldingPosition::assetType)
            .containsExactly(
                AssetType.STOCK,
                AssetType.ETF,
                AssetType.MUTUAL_FUND,
                AssetType.CRYPTO,
                AssetType.CASH,
                AssetType.BANK_DEPOSIT
            );
        assertThat(snapshot.holdings())
            .filteredOn(holding -> holding.assetType().isFixedValueAsset())
            .allSatisfy(holding -> assertThat(holding.averagePurchasePrice()).isEqualByComparingTo(BigDecimal.ONE));
    }
}
