package com.holdhive.analysis.domain;

import com.holdhive.analysis.domain.ProfitLossCalculator.ProfitLossResult;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.HoldingFact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfitLossCalculatorTest {

    private final ProfitLossCalculator calculator = new ProfitLossCalculator();

    @Test
    void computesUnrealizedPnlAndPercent() {
        List<HoldingFact> holdings = List.of(
                fact("600519", AssetType.STOCK, "170000", "150000"),
                fact("000858", AssetType.STOCK, "80000", "100000"));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.holdings()).hasSize(2);
        assertThat(r.holdings().get(0).unrealizedPnl()).isEqualByComparingTo("20000.00");
        assertThat(r.holdings().get(0).unrealizedPnlPercent()).isEqualByComparingTo("13.33");
        assertThat(r.holdings().get(1).unrealizedPnl()).isEqualByComparingTo("-20000.00");
        assertThat(r.holdings().get(1).unrealizedPnlPercent()).isEqualByComparingTo("-20.00");
        assertThat(r.totalCostBasis()).isEqualByComparingTo("250000.00");
        assertThat(r.totalUnrealizedPnl()).isEqualByComparingTo("0.00");
    }

    @Test
    void identifiesBestAndWorstPerformers() {
        List<HoldingFact> holdings = List.of(
                fact("A", AssetType.STOCK, "12000", "10000"),
                fact("B", AssetType.STOCK, "9000", "10000"));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.bestPerformerTicker()).isEqualTo("A");
        assertThat(r.bestPerformerPnlPercent()).isEqualByComparingTo("20.00");
        assertThat(r.worstPerformerTicker()).isEqualTo("B");
        assertThat(r.worstPerformerPnlPercent()).isEqualByComparingTo("-10.00");
    }

    @Test
    void reportsMissingCostBasisSeparately() {
        List<HoldingFact> holdings = List.of(
                fact("600519", AssetType.STOCK, "5000", null),
                fact("CASH", AssetType.CASH, "1000", "1000"));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.missingCostBasisTickers()).containsExactly("600519");
        assertThat(r.holdings()).hasSize(1);
        assertThat(r.holdings().get(0).ticker()).isEqualTo("CASH");
    }

    @Test
    void handlesZeroCostBasis() {
        List<HoldingFact> holdings = List.of(
                fact("GIFT", AssetType.STOCK, "5000", "0"));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.holdings()).hasSize(1);
        assertThat(r.holdings().get(0).unrealizedPnlPercent()).isNull();
        assertThat(r.totalUnrealizedPnlPercent()).isNull();
    }

    @Test
    void handlesAllMissingCostBasis() {
        List<HoldingFact> holdings = List.of(
                fact("A", AssetType.STOCK, "1000", null));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.holdings()).isEmpty();
        assertThat(r.missingCostBasisTickers()).containsExactly("A");
    }

    // --- realistic portfolio scenarios ---

    @Test
    void scenarioExtremeConcentration() {
        List<HoldingFact> holdings = List.of(
                fact("600519", AssetType.STOCK, "180000", "200000"),
                fact("CASH", AssetType.CASH, "20000", "20000"));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.totalCostBasis()).isEqualByComparingTo("220000.00");
        assertThat(r.totalUnrealizedPnl()).isEqualByComparingTo("-20000.00");
        assertThat(r.totalUnrealizedPnlPercent()).isEqualByComparingTo("-9.09");
        assertThat(r.bestPerformerTicker()).isEqualTo("CASH");
        assertThat(r.bestPerformerPnlPercent()).isEqualByComparingTo("0.00");
        assertThat(r.worstPerformerTicker()).isEqualTo("600519");
        assertThat(r.worstPerformerPnlPercent()).isEqualByComparingTo("-10.00");
        assertThat(r.missingCostBasisTickers()).isEmpty();
    }

    @Test
    void scenarioModerateFourStocks() {
        List<HoldingFact> holdings = List.of(
                fact("600519", AssetType.STOCK, "50000", "45000"),
                fact("300750", AssetType.STOCK, "40000", "35000"),
                fact("600036", AssetType.STOCK, "35000", "38000"),
                fact("000333", AssetType.STOCK, "25000", "28000"),
                fact("CASH", AssetType.CASH, "50000", "50000"));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.totalCostBasis()).isEqualByComparingTo("196000.00");
        assertThat(r.totalUnrealizedPnl()).isEqualByComparingTo("4000.00");
        assertThat(r.totalUnrealizedPnlPercent()).isEqualByComparingTo("2.04");
        assertThat(r.bestPerformerTicker()).isEqualTo("300750");
        assertThat(r.bestPerformerPnlPercent()).isEqualByComparingTo("14.29");
        assertThat(r.worstPerformerTicker()).isEqualTo("000333");
        assertThat(r.worstPerformerPnlPercent()).isEqualByComparingTo("-10.71");
    }

    @Test
    void scenarioPureFunds() {
        List<HoldingFact> holdings = List.of(
                fact("005827", AssetType.FUND, "100000", "90000"),
                fact("000001", AssetType.FUND, "60000", "65000"),
                fact("CASH", AssetType.CASH, "40000", "40000"));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.totalCostBasis()).isEqualByComparingTo("195000.00");
        assertThat(r.totalUnrealizedPnl()).isEqualByComparingTo("5000.00");
        assertThat(r.totalUnrealizedPnlPercent()).isEqualByComparingTo("2.56");
        assertThat(r.bestPerformerTicker()).isEqualTo("005827");
        assertThat(r.bestPerformerPnlPercent()).isEqualByComparingTo("11.11");
        assertThat(r.worstPerformerTicker()).isEqualTo("000001");
        assertThat(r.worstPerformerPnlPercent()).isEqualByComparingTo("-7.69");
    }

    @Test
    void scenarioPseudoDiversified() {
        List<HoldingFact> holdings = List.of(
                fact("600519", AssetType.STOCK, "170000", "150000"),
                fact("000858", AssetType.STOCK, "80000", "100000"),
                fact("005827", AssetType.FUND, "50000", "48000"),
                fact("000001", AssetType.FUND, "30000", null),
                fact("999999", AssetType.FUND, "10000", null),
                fact("CASH", AssetType.CASH, "20000", "20000"));

        ProfitLossResult r = calculator.calculate(holdings);

        assertThat(r.totalCostBasis()).isEqualByComparingTo("318000.00");
        assertThat(r.totalUnrealizedPnl()).isEqualByComparingTo("2000.00");
        assertThat(r.totalUnrealizedPnlPercent()).isEqualByComparingTo("0.63");
        assertThat(r.bestPerformerTicker()).isEqualTo("600519");
        assertThat(r.bestPerformerPnlPercent()).isEqualByComparingTo("13.33");
        assertThat(r.worstPerformerTicker()).isEqualTo("000858");
        assertThat(r.worstPerformerPnlPercent()).isEqualByComparingTo("-20.00");
        assertThat(r.missingCostBasisTickers()).containsExactly("000001", "999999");
    }

    private HoldingFact fact(String ticker, AssetType type, String mv, String cost) {
        return new HoldingFact(ticker, type, BigDecimal.ONE, new BigDecimal(mv),
                cost != null ? new BigDecimal(cost) : null);
    }
}
