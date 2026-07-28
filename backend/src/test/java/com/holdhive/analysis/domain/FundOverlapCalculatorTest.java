package com.holdhive.analysis.domain;

import com.holdhive.analysis.domain.FundOverlapCalculator.FundOverlapResult;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.FundConstituent;
import com.holdhive.analysis.domain.model.FundHoldingSnapshot;
import com.holdhive.analysis.domain.model.HoldingFact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FundOverlapCalculatorTest {

    private final FundOverlapCalculator calculator = new FundOverlapCalculator();

    @Test
    void detectsOverlapBetweenFundHoldingsAndDirectStocks() {
        List<HoldingFact> holdings = List.of(
                new HoldingFact("600519", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("5000"), null),
                new HoldingFact("000001", AssetType.FUND, BigDecimal.ONE, new BigDecimal("5000"), null));

        FundHoldingsLookup lookup = fundTicker -> fundTicker.equals("000001")
                ? Optional.of(new FundHoldingSnapshot("000001", "Demo Fund", "2024Q1",
                        List.of(new FundConstituent("600519", "贵州茅台", new BigDecimal("3.20"), null))))
                : Optional.empty();

        FundOverlapResult result = calculator.calculate(holdings, lookup);

        assertThat(result.funds()).hasSize(1);
        assertThat(result.funds().get(0).overlapStocks()).hasSize(1);
        assertThat(result.funds().get(0).overlapMarketValue()).isEqualByComparingTo("5000.00");
        assertThat(result.totalOverlapMarketValue()).isEqualByComparingTo("5000.00");
        assertThat(result.totalOverlapPercentOfPortfolio()).isEqualByComparingTo("50.00");
        assertThat(result.unavailableFunds()).isEmpty();
    }

    @Test
    void reportsUnavailableFundsSeparately() {
        List<HoldingFact> holdings = List.of(
                new HoldingFact("999999", AssetType.FUND, BigDecimal.ONE, new BigDecimal("1000"), null));

        FundOverlapResult result = calculator.calculate(holdings, fundTicker -> Optional.empty());

        assertThat(result.funds()).isEmpty();
        assertThat(result.unavailableFunds()).hasSize(1);
        assertThat(result.unavailableFunds().get(0).fundTicker()).isEqualTo("999999");
        assertThat(result.unavailableFunds().get(0).reason()).isEqualTo("FUND_DATA_UNAVAILABLE");
    }

    @Test
    void handlesNoOverlapWithoutError() {
        List<HoldingFact> holdings = List.of(
                new HoldingFact("000333", AssetType.STOCK, BigDecimal.ONE, new BigDecimal("1000"), null),
                new HoldingFact("000001", AssetType.FUND, BigDecimal.ONE, new BigDecimal("1000"), null));

        FundHoldingsLookup lookup = fundTicker -> Optional.of(new FundHoldingSnapshot(
                "000001", "Demo Fund", "2024Q1",
                List.of(new FundConstituent("600519", "贵州茅台", new BigDecimal("3.20"), null))));

        FundOverlapResult result = calculator.calculate(holdings, lookup);

        assertThat(result.funds()).hasSize(1);
        assertThat(result.funds().get(0).overlapStocks()).isEmpty();
        assertThat(result.totalOverlapMarketValue()).isEqualByComparingTo("0.00");
    }
}
