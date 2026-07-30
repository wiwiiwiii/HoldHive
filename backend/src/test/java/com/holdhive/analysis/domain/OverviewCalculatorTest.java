package com.holdhive.analysis.domain;

import com.holdhive.analysis.domain.OverviewCalculator.OverviewResult;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.HoldingFact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OverviewCalculatorTest {

    private final OverviewCalculator calculator = new OverviewCalculator();

    @Test
    void computesTotalAndAllocationByAssetType() {
        List<HoldingFact> holdings = List.of(
                fact("600519", AssetType.STOCK, "6000"),
                fact("000001", AssetType.FUND, "3000"),
                fact("CASH", AssetType.CASH, "1000"));

        OverviewResult result = calculator.calculate(holdings);

        assertThat(result.totalMarketValue()).isEqualByComparingTo("10000");
        assertThat(result.allocations()).hasSize(3);
        assertThat(result.allocations().get(0).assetType()).isEqualTo(AssetType.STOCK);
        assertThat(result.allocations().get(0).percent()).isEqualByComparingTo("60.00");
    }

    @Test
    void returnsZeroForEmptyPortfolio() {
        OverviewResult result = calculator.calculate(List.of());

        assertThat(result.totalMarketValue()).isEqualByComparingTo("0");
        assertThat(result.allocations()).isEmpty();
    }

    private HoldingFact fact(String ticker, AssetType type, String marketValue) {
        return new HoldingFact(ticker, type, BigDecimal.ONE, new BigDecimal(marketValue), null);
    }
}
