package com.holdhive.portfolio.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.holdhive.pricing.domain.PriceStatus;

class PortfolioCalculatorTest {

    private final PortfolioCalculator calculator = new PortfolioCalculator();

    @Test
    void returnsEmptyValuationForPortfolioWithoutHoldings() {
        PortfolioValuation valuation = calculator.calculate(List.of());

        assertThat(valuation.valuationStatus()).isEqualTo(ValuationStatus.EMPTY);
        assertThat(valuation.holdingCount()).isZero();
        assertThat(valuation.pricedHoldingCount()).isZero();
        assertThat(valuation.totalCostBasis()).isEqualByComparingTo("0.00000000");
        assertThat(valuation.totalMarketValue()).isEqualByComparingTo("0.00000000");
        assertThat(valuation.totalUnrealizedGainLoss()).isEqualByComparingTo("0.00000000");
        assertThat(valuation.totalUnrealizedGainLossPercent()).isNull();
        assertThat(valuation.allocations()).isEmpty();
        assertThat(valuation.unpricedHoldings()).isEmpty();
    }

    @Test
    void calculatesCompleteValuationForPricedHoldings() {
        Instant observedAt = Instant.parse("2026-07-24T08:29:00Z");

        PortfolioValuation valuation = calculator.calculate(List.of(
            new HoldingValuationInput(
                101L,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("175.50"),
                new BigDecimal("210.25"),
                PriceStatus.LIVE,
                observedAt
            ),
            new HoldingValuationInput(
                102L,
                "MSFT",
                new BigDecimal("5"),
                new BigDecimal("300.00"),
                new BigDecimal("330.00"),
                PriceStatus.CACHED,
                observedAt
            )
        ));

        assertThat(valuation.valuationStatus()).isEqualTo(ValuationStatus.COMPLETE);
        assertThat(valuation.holdingCount()).isEqualTo(2);
        assertThat(valuation.pricedHoldingCount()).isEqualTo(2);
        assertThat(valuation.totalCostBasis()).isEqualByComparingTo("3255.00000000");
        assertThat(valuation.totalMarketValue()).isEqualByComparingTo("3752.50000000");
        assertThat(valuation.totalUnrealizedGainLoss()).isEqualByComparingTo("497.50000000");
        assertThat(valuation.totalUnrealizedGainLossPercent()).isEqualByComparingTo("15.28417819");
        assertThat(valuation.priceAsOf()).isEqualTo(observedAt);
        assertThat(valuation.allocations()).hasSize(2);
        assertThat(valuation.allocations().getFirst().allocationPercent()).isEqualByComparingTo("56.02931379");
        assertThat(valuation.allocations().get(1).allocationPercent()).isEqualByComparingTo("43.97068621");
        assertThat(valuation.unpricedHoldings()).isEmpty();
    }

    @Test
    void excludesUnpricedHoldingsFromMarketValueAndAllocationsButKeepsCostBasis() {
        Instant observedAt = Instant.parse("2026-07-24T08:29:00Z");

        PortfolioValuation valuation = calculator.calculate(List.of(
            new HoldingValuationInput(
                101L,
                "AAPL",
                new BigDecimal("10"),
                new BigDecimal("175.50"),
                new BigDecimal("210.25"),
                PriceStatus.LIVE,
                observedAt
            ),
            new HoldingValuationInput(
                103L,
                "UNKNOWN",
                new BigDecimal("2"),
                new BigDecimal("500.00"),
                null,
                PriceStatus.UNAVAILABLE,
                null
            )
        ));

        assertThat(valuation.valuationStatus()).isEqualTo(ValuationStatus.PARTIAL);
        assertThat(valuation.holdingCount()).isEqualTo(2);
        assertThat(valuation.pricedHoldingCount()).isEqualTo(1);
        assertThat(valuation.totalCostBasis()).isEqualByComparingTo("2755.00000000");
        assertThat(valuation.totalMarketValue()).isEqualByComparingTo("2102.50000000");
        assertThat(valuation.totalUnrealizedGainLoss()).isEqualByComparingTo("347.50000000");
        assertThat(valuation.totalUnrealizedGainLossPercent()).isEqualByComparingTo("12.61343013");
        assertThat(valuation.allocations()).singleElement()
            .satisfies(allocation -> {
                assertThat(allocation.holdingId()).isEqualTo(101L);
                assertThat(allocation.allocationPercent()).isEqualByComparingTo("100.00000000");
            });
        assertThat(valuation.unpricedHoldings()).singleElement()
            .satisfies(unpriced -> {
                assertThat(unpriced.holdingId()).isEqualTo(103L);
                assertThat(unpriced.ticker()).isEqualTo("UNKNOWN");
                assertThat(unpriced.reason()).isEqualTo("PRICE_UNAVAILABLE");
            });
    }

    @Test
    void doesNotCalculateGainLossPercentWhenCostBasisIsZero() {
        PortfolioValuation valuation = calculator.calculate(List.of(
            new HoldingValuationInput(
                101L,
                "FREE",
                new BigDecimal("10"),
                BigDecimal.ZERO,
                new BigDecimal("20.00"),
                PriceStatus.DEMO,
                Instant.parse("2026-07-24T08:29:00Z")
            )
        ));

        assertThat(valuation.valuationStatus()).isEqualTo(ValuationStatus.COMPLETE);
        assertThat(valuation.totalCostBasis()).isEqualByComparingTo("0.00000000");
        assertThat(valuation.totalMarketValue()).isEqualByComparingTo("200.00000000");
        assertThat(valuation.totalUnrealizedGainLoss()).isEqualByComparingTo("200.00000000");
        assertThat(valuation.totalUnrealizedGainLossPercent()).isNull();
    }
}
