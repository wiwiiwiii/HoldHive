package com.holdhive.analysis.domain;

import com.holdhive.analysis.domain.ConcentrationCalculator.RiskLevel;
import com.holdhive.analysis.domain.LookThroughCalculator.LookThroughResult;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.FundConstituent;
import com.holdhive.analysis.domain.model.FundHoldingSnapshot;
import com.holdhive.analysis.domain.model.HoldingFact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LookThroughCalculatorTest {

    private final LookThroughCalculator calculator = new LookThroughCalculator();

    @Test
    void aggregatesDirectAndIndirectExposures() {
        List<HoldingFact> holdings = List.of(
                fact("600519", AssetType.STOCK, "5000"),
                fact("000001", AssetType.FUND, "5000"));

        FundHoldingsLookup lookup = ticker -> ticker.equals("000001")
                ? Optional.of(snapshot("000001", List.of(
                        c("600519", "贵州茅台", "3.20"))))
                : Optional.empty();

        LookThroughResult r = calculator.calculate(holdings, lookup);

        assertThat(r.exposures()).hasSize(1);
        assertThat(r.exposures().get(0).directMarketValue()).isEqualByComparingTo("5000.00");
        assertThat(r.exposures().get(0).indirectMarketValue()).isEqualByComparingTo("160.00");
        assertThat(r.exposures().get(0).effectiveMarketValue()).isEqualByComparingTo("5160.00");
        assertThat(r.exposures().get(0).effectivePercentOfPortfolio()).isEqualByComparingTo("51.60");
    }

    @Test
    void includesAllFundConstituentsNotJustOverlapping() {
        List<HoldingFact> holdings = List.of(
                fact("000001", AssetType.FUND, "5000"));

        FundHoldingsLookup lookup = ticker -> Optional.of(snapshot("000001", List.of(
                c("600519", "茅台", "3.00"),
                c("000333", "美的", "2.00"))));

        LookThroughResult r = calculator.calculate(holdings, lookup);

        assertThat(r.exposures()).hasSize(2);
        assertThat(r.exposures().get(0).name()).isEqualTo("茅台");
    }

    @Test
    void treatsFundWithoutDataAsOpaqueBucket() {
        List<HoldingFact> holdings = List.of(
                fact("999999", AssetType.FUND, "1000"),
                fact("CASH", AssetType.CASH, "1000"));

        LookThroughResult r = calculator.calculate(holdings, ticker -> Optional.empty());

        assertThat(r.exposures()).isEmpty();
        assertThat(r.attributedPercentOfPortfolio()).isEqualByComparingTo("0.00");
    }

    @Test
    void effectiveHhiMatchesConcentrationHhiWhenNoFunds() {
        List<HoldingFact> holdings = List.of(
                fact("A", AssetType.STOCK, "5000"),
                fact("B", AssetType.STOCK, "5000"));

        LookThroughResult r = calculator.calculate(holdings, ticker -> Optional.empty());

        assertThat(r.effectiveHhi()).isEqualByComparingTo("0.5000");
    }

    @Test
    void handlesEmptyPortfolio() {
        LookThroughResult r = calculator.calculate(List.of(), ticker -> Optional.empty());

        assertThat(r.exposures()).isEmpty();
        assertThat(r.effectiveHhi()).isEqualByComparingTo("0.0000");
        assertThat(r.effectiveRiskLevel()).isEqualTo(RiskLevel.LOW);
    }

    // --- realistic portfolio scenarios ---

    private static final FundHoldingsLookup MOCK_LOOKUP = ticker -> {
        if ("000001".equals(ticker)) {
            return Optional.of(new FundHoldingSnapshot("000001", "华夏成长", "2024Q1", List.of(
                    new FundConstituent("600519", "贵州茅台", new BigDecimal("3.20"), null),
                    new FundConstituent("601318", "中国平安", new BigDecimal("2.80"), null),
                    new FundConstituent("000858", "五粮液", new BigDecimal("2.10"), null),
                    new FundConstituent("300750", "宁德时代", new BigDecimal("4.50"), null),
                    new FundConstituent("002475", "立讯精密", new BigDecimal("2.30"), null))));
        }
        if ("005827".equals(ticker)) {
            return Optional.of(new FundHoldingSnapshot("005827", "易方达蓝筹", "2024Q1", List.of(
                    new FundConstituent("600519", "贵州茅台", new BigDecimal("8.50"), null),
                    new FundConstituent("000858", "五粮液", new BigDecimal("5.20"), null),
                    new FundConstituent("600036", "招商银行", new BigDecimal("4.10"), null),
                    new FundConstituent("000333", "美的集团", new BigDecimal("3.60"), null),
                    new FundConstituent("601888", "中国中免", new BigDecimal("2.90"), null))));
        }
        return Optional.empty();
    };

    @Test
    void scenarioExtremeConcentration() {
        List<HoldingFact> holdings = List.of(
                new HoldingFact("600519", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("180000"), null),
                new HoldingFact("CASH", AssetType.CASH, BigDecimal.ONE, new BigDecimal("20000"), null));

        LookThroughResult r = calculator.calculate(holdings, MOCK_LOOKUP);

        assertThat(r.exposures()).hasSize(1);
        assertThat(r.exposures().get(0).ticker()).isEqualTo("600519");
        assertThat(r.exposures().get(0).effectivePercentOfPortfolio()).isEqualByComparingTo("90.00");
        assertThat(r.effectiveHhi()).isEqualByComparingTo("0.8200");
        assertThat(r.effectiveRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.topHoldingTicker()).isEqualTo("600519");
        assertThat(r.topHoldingPercent()).isEqualByComparingTo("90.00");
        assertThat(r.attributedPercentOfPortfolio()).isEqualByComparingTo("90.00");
    }

    @Test
    void scenarioModerateFourStocks() {
        List<HoldingFact> holdings = List.of(
                new HoldingFact("600519", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("50000"), null),
                new HoldingFact("300750", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("40000"), null),
                new HoldingFact("600036", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("35000"), null),
                new HoldingFact("000333", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("25000"), null),
                new HoldingFact("CASH", AssetType.CASH, BigDecimal.ONE, new BigDecimal("50000"), null));

        LookThroughResult r = calculator.calculate(holdings, MOCK_LOOKUP);

        assertThat(r.exposures()).hasSize(4);
        assertThat(r.effectiveHhi()).isEqualByComparingTo("0.2113");
        assertThat(r.effectiveRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(r.topHoldingTicker()).isEqualTo("600519");
        assertThat(r.topHoldingPercent()).isEqualByComparingTo("25.00");
        assertThat(r.attributedPercentOfPortfolio()).isEqualByComparingTo("75.00");
    }

    @Test
    void scenarioPureFundsLookThrough() {
        List<HoldingFact> holdings = List.of(
                new HoldingFact("005827", AssetType.FUND, BigDecimal.ONE, new BigDecimal("100000"), null),
                new HoldingFact("000001", AssetType.FUND, BigDecimal.ONE, new BigDecimal("60000"), null),
                new HoldingFact("CASH", AssetType.CASH, BigDecimal.ONE, new BigDecimal("40000"), null));

        LookThroughResult r = calculator.calculate(holdings, MOCK_LOOKUP);

        assertThat(r.exposures()).hasSize(8);
        assertThat(r.effectiveHhi()).isEqualByComparingTo("0.2535");
        assertThat(r.effectiveRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.attributedPercentOfPortfolio()).isEqualByComparingTo("16.62");
        assertThat(r.exposures().get(0).ticker()).isEqualTo("600519");
        assertThat(r.exposures().get(0).effectivePercentOfPortfolio()).isEqualByComparingTo("5.21");
    }

    @Test
    void scenarioPseudoDiversified() {
        List<HoldingFact> holdings = List.of(
                new HoldingFact("600519", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("170000"), null),
                new HoldingFact("000858", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("80000"), null),
                new HoldingFact("005827", AssetType.FUND, BigDecimal.ONE, new BigDecimal("50000"), null),
                new HoldingFact("000001", AssetType.FUND, BigDecimal.ONE, new BigDecimal("30000"), null),
                new HoldingFact("999999", AssetType.FUND, BigDecimal.ONE, new BigDecimal("10000"), null),
                new HoldingFact("CASH", AssetType.CASH, BigDecimal.ONE, new BigDecimal("20000"), null));

        LookThroughResult r = calculator.calculate(holdings, MOCK_LOOKUP);

        assertThat(r.exposures()).hasSize(8);
        assertThat(r.effectiveHhi()).isEqualByComparingTo("0.3104");
        assertThat(r.effectiveRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.topHoldingTicker()).isEqualTo("600519");
        assertThat(r.topHoldingPercent()).isEqualByComparingTo("48.67");
        assertThat(r.attributedPercentOfPortfolio()).isEqualByComparingTo("74.06");
    }

    private HoldingFact fact(String ticker, AssetType type, String mv) {
        return new HoldingFact(ticker, type, BigDecimal.ONE, new BigDecimal(mv), null);
    }

    private FundHoldingSnapshot snapshot(String ticker, List<FundConstituent> constituents) {
        return new FundHoldingSnapshot(ticker, "Demo", "2024Q1", constituents);
    }

    private FundConstituent c(String ticker, String name, String weight) {
        return new FundConstituent(ticker, name, new BigDecimal(weight), null);
    }
}
