package com.holdhive.analysis.domain;

import com.holdhive.analysis.domain.ConcentrationCalculator.ConcentrationResult;
import com.holdhive.analysis.domain.ConcentrationCalculator.RiskLevel;
import com.holdhive.analysis.domain.ConcentrationCalculator.TopHolding;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.HoldingFact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConcentrationCalculatorTest {

    private final ConcentrationCalculator calculator = new ConcentrationCalculator();

    @Test
    void flagsHighConcentrationWhenOneHoldingDominates() {
        List<HoldingFact> holdings = List.of(
                fact("600519", "9000"),
                fact("000858", "1000"));

        ConcentrationResult result = calculator.calculate(holdings);

        assertThat(result.topHoldingTicker()).isEqualTo("600519");
        assertThat(result.topHoldingPercent()).isEqualByComparingTo("90.00");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void flagsLowConcentrationWhenEvenlySpread() {
        List<HoldingFact> holdings = List.of(
                fact("A", "2500"),
                fact("B", "2500"),
                fact("C", "2500"),
                fact("D", "2500"));

        ConcentrationResult result = calculator.calculate(holdings);

        assertThat(result.hhi()).isEqualByComparingTo("0.2500");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void handlesEmptyPortfolioWithoutDividingByZero() {
        ConcentrationResult result = calculator.calculate(List.of());

        assertThat(result.topHoldingTicker()).isNull();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.holdingCount()).isZero();
        assertThat(result.topHoldings()).isEmpty();
        assertThat(result.topHoldingsCombinedPercent()).isEqualByComparingTo("0.00");
    }

    @Test
    void reportsTopHoldingsWithCombinedPercentAndCount() {
        List<HoldingFact> holdings = List.of(
                fact("600519", "50000"),
                fact("300750", "40000"),
                fact("600036", "35000"),
                fact("000333", "25000"),
                new HoldingFact("CASH", AssetType.CASH, BigDecimal.ONE, new BigDecimal("50000"), null));

        ConcentrationResult result = calculator.calculate(holdings);

        assertThat(result.holdingCount()).isEqualTo(5);
        assertThat(result.topHoldings())
                .extracting(TopHolding::ticker)
                .containsExactly("600519", "CASH", "300750", "600036", "000333");
        assertThat(result.topHoldings().get(0).assetType()).isEqualTo(AssetType.STOCK);
        assertThat(result.topHoldings().get(0).percentOfPortfolio()).isEqualByComparingTo("25.00");
        assertThat(result.topHoldings().get(1).assetType()).isEqualTo(AssetType.CASH);
        assertThat(result.topHoldingsCombinedPercent()).isEqualByComparingTo("100.00");
    }

    @Test
    void capsTopHoldingsAtFiveAndSumsOnlyThose() {
        List<HoldingFact> holdings = List.of(
                fact("A", "3000"),
                fact("B", "2500"),
                fact("C", "2000"),
                fact("D", "1500"),
                fact("E", "500"),
                fact("F", "500"));

        ConcentrationResult result = calculator.calculate(holdings);

        assertThat(result.holdingCount()).isEqualTo(6);
        assertThat(result.topHoldings()).hasSize(5);
        assertThat(result.topHoldingsCombinedPercent()).isEqualByComparingTo("95.00");
    }

    private HoldingFact fact(String ticker, String marketValue) {
        return new HoldingFact(ticker, AssetType.STOCK, BigDecimal.ONE, new BigDecimal(marketValue), null);
    }
}
