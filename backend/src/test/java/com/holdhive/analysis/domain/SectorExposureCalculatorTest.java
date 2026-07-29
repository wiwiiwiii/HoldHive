package com.holdhive.analysis.domain;

import com.holdhive.analysis.domain.ConcentrationCalculator.RiskLevel;
import com.holdhive.analysis.domain.SectorExposureCalculator.SectorExposureResult;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.FundConstituent;
import com.holdhive.analysis.domain.model.FundHoldingSnapshot;
import com.holdhive.analysis.domain.model.HoldingFact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SectorExposureCalculatorTest {

    private final SectorExposureCalculator calculator = new SectorExposureCalculator();

    private static final Map<String, String> TEST_SECTORS = Map.of(
            "600519", "食品饮料",
            "000858", "食品饮料",
            "300750", "电力设备",
            "600036", "银行",
            "000333", "家用电器",
            "601318", "金融保险",
            "002475", "电子",
            "601888", "商贸零售"
    );

    private static final SectorLookup TEST_SECTOR_LOOKUP =
            ticker -> Optional.ofNullable(TEST_SECTORS.getOrDefault(ticker, null));

    private static final FundHoldingsLookup TEST_FUND_LOOKUP = ticker -> {
        if ("000001".equals(ticker)) {
            return Optional.of(new FundHoldingSnapshot("000001", "华夏成长", "2024Q1", List.of(
                    c("600519", "贵州茅台", "3.20", "食品饮料"),
                    c("601318", "中国平安", "2.80", "金融保险"),
                    c("000858", "五粮液", "2.10", "食品饮料"),
                    c("300750", "宁德时代", "4.50", "电力设备"),
                    c("002475", "立讯精密", "2.30", "电子"))));
        }
        if ("005827".equals(ticker)) {
            return Optional.of(new FundHoldingSnapshot("005827", "易方达蓝筹", "2024Q1", List.of(
                    c("600519", "贵州茅台", "8.50", "食品饮料"),
                    c("000858", "五粮液", "5.20", "食品饮料"),
                    c("600036", "招商银行", "4.10", "银行"),
                    c("000333", "美的集团", "3.60", "家用电器"),
                    c("601888", "中国中免", "2.90", "商贸零售"))));
        }
        return Optional.empty();
    };

    @Test
    void calculatesDirectSectorExposure() {
        List<HoldingFact> holdings = List.of(
                f("600519", AssetType.STOCK, "50000"),
                f("300750", AssetType.STOCK, "30000"),
                f("600036", AssetType.STOCK, "20000"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, ticker -> Optional.empty());

        assertThat(r.sectors()).hasSize(3);
        assertThat(r.sectors().get(0).sector()).isEqualTo("食品饮料");
        assertThat(r.sectors().get(0).effectivePercentOfPortfolio()).isEqualByComparingTo("50.00");
        assertThat(r.topSector()).isEqualTo("食品饮料");
        assertThat(r.topSectorPercent()).isEqualByComparingTo("50.00");
        assertThat(r.attributedPercentOfPortfolio()).isEqualByComparingTo("100.00");
    }

    @Test
    void mergesSameSectorAcrossMultipleStocks() {
        List<HoldingFact> holdings = List.of(
                f("600519", AssetType.STOCK, "50000"),
                f("000858", AssetType.STOCK, "30000"),
                f("600036", AssetType.STOCK, "20000"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, ticker -> Optional.empty());

        assertThat(r.sectors()).hasSize(2);
        assertThat(r.sectors().get(0).sector()).isEqualTo("食品饮料");
        assertThat(r.sectors().get(0).effectiveMarketValue()).isEqualByComparingTo("80000.00");
        assertThat(r.sectors().get(0).effectivePercentOfPortfolio()).isEqualByComparingTo("80.00");
    }

    @Test
    void includesLookThroughSectorFromFunds() {
        List<HoldingFact> holdings = List.of(
                f("005827", AssetType.FUND, "100000"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, TEST_FUND_LOOKUP);

        assertThat(r.sectors()).isNotEmpty();
        boolean hasFoodBeverage = r.sectors().stream()
                .anyMatch(s -> "食品饮料".equals(s.sector()) && s.indirectMarketValue().compareTo(BigDecimal.ZERO) > 0);
        assertThat(hasFoodBeverage).isTrue();
    }

    @Test
    void mergesDirectAndIndirectSectorExposure() {
        List<HoldingFact> holdings = List.of(
                f("600519", AssetType.STOCK, "50000"),
                f("005827", AssetType.FUND, "50000"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, TEST_FUND_LOOKUP);

        SectorExposureCalculator.SectorAllocation food = r.sectors().stream()
                .filter(s -> "食品饮料".equals(s.sector()))
                .findFirst().orElseThrow();

        assertThat(food.directMarketValue()).isEqualByComparingTo("50000.00");
        assertThat(food.indirectMarketValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(food.effectiveMarketValue()).isGreaterThan(food.directMarketValue());
    }

    @Test
    void flagsUnknownStockSectors() {
        List<HoldingFact> holdings = List.of(
                f("UNKNOWN", AssetType.STOCK, "10000"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, ticker -> Optional.empty());

        assertThat(r.sectors()).hasSize(1);
        assertThat(r.sectors().get(0).sector()).isEqualTo(SectorExposureCalculator.BUCKET_UNKNOWN_SECTOR);
    }

    @Test
    void handlesNonEquityHoldings() {
        List<HoldingFact> holdings = List.of(
                f("CASH", AssetType.CASH, "30000"),
                f("600519", AssetType.STOCK, "70000"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, ticker -> Optional.empty());

        assertThat(r.sectors()).hasSize(2);
        assertThat(r.attributedPercentOfPortfolio()).isLessThan(new BigDecimal("100.01"));
    }

    @Test
    void calculatesSectorHhi() {
        List<HoldingFact> holdings = List.of(
                f("600519", AssetType.STOCK, "90000"),
                f("600036", AssetType.STOCK, "10000"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, ticker -> Optional.empty());

        assertThat(r.sectorHhi()).isEqualByComparingTo("0.8200");
        assertThat(r.sectorRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void flagsLowSectorConcentration() {
        List<HoldingFact> holdings = List.of(
                f("600519", AssetType.STOCK, "12500"),
                f("300750", AssetType.STOCK, "12500"),
                f("600036", AssetType.STOCK, "12500"),
                f("000333", AssetType.STOCK, "12500"),
                f("601318", AssetType.STOCK, "12500"),
                f("002475", AssetType.STOCK, "12500"),
                f("601888", AssetType.STOCK, "12500"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, ticker -> Optional.empty());

        assertThat(r.sectors()).hasSize(7);
        assertThat(r.sectorRiskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void handlesEmptyPortfolio() {
        SectorExposureResult r = calculator.calculate(List.of(), TEST_SECTOR_LOOKUP, ticker -> Optional.empty());

        assertThat(r.sectors()).isEmpty();
        assertThat(r.sectorHhi()).isEqualByComparingTo("0.0000");
        assertThat(r.sectorRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(r.topSector()).isNull();
    }

    @Test
    void topSectorIsLargestEffectiveExposure() {
        List<HoldingFact> holdings = List.of(
                f("600519", AssetType.STOCK, "60000"),
                f("300750", AssetType.STOCK, "40000"));

        SectorExposureResult r = calculator.calculate(holdings, TEST_SECTOR_LOOKUP, ticker -> Optional.empty());

        assertThat(r.topSector()).isEqualTo("食品饮料");
        assertThat(r.topSectorPercent()).isEqualByComparingTo("60.00");
    }

    private HoldingFact f(String ticker, AssetType type, String mv) {
        return new HoldingFact(ticker, type, BigDecimal.ONE, new BigDecimal(mv), null);
    }

    private static FundConstituent c(String ticker, String name, String weight, String sector) {
        return new FundConstituent(ticker, name, new BigDecimal(weight), sector);
    }
}
