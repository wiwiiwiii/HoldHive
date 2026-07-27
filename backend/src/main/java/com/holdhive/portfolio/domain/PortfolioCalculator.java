package com.holdhive.portfolio.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.holdhive.pricing.domain.PriceStatus;

public class PortfolioCalculator {

    private static final int SCALE = 8;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = scaled(BigDecimal.ZERO);

    public PortfolioValuation calculate(List<HoldingValuationInput> holdings) {
        if (holdings == null || holdings.isEmpty()) {
            return new PortfolioValuation(
                0,
                0,
                ValuationStatus.EMPTY,
                ZERO,
                ZERO,
                ZERO,
                null,
                null,
                List.of(),
                List.of()
            );
        }

        List<PricedHolding> pricedHoldings = new ArrayList<>();
        List<UnpricedHolding> unpricedHoldings = new ArrayList<>();
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalUnrealizedGainLoss = BigDecimal.ZERO;

        for (HoldingValuationInput holding : holdings) {
            BigDecimal costBasis = holding.quantity().multiply(holding.averagePurchasePrice());
            totalCostBasis = totalCostBasis.add(costBasis);

            if (isPriced(holding)) {
                BigDecimal marketValue = holding.quantity().multiply(holding.currentPrice());
                BigDecimal gainLoss = marketValue.subtract(costBasis);
                totalMarketValue = totalMarketValue.add(marketValue);
                totalUnrealizedGainLoss = totalUnrealizedGainLoss.add(gainLoss);
                pricedHoldings.add(new PricedHolding(holding, marketValue));
            } else {
                unpricedHoldings.add(new UnpricedHolding(
                    holding.holdingId(),
                    holding.ticker(),
                    "PRICE_UNAVAILABLE"
                ));
            }
        }

        ValuationStatus valuationStatus = valuationStatus(holdings.size(), pricedHoldings.size());
        List<PortfolioAllocation> allocations = allocations(pricedHoldings, totalMarketValue);
        Instant priceAsOf = pricedHoldings.stream()
            .map(priced -> priced.holding().priceObservedAt())
            .filter(observedAt -> observedAt != null)
            .max(Comparator.naturalOrder())
            .orElse(null);

        return new PortfolioValuation(
            holdings.size(),
            pricedHoldings.size(),
            valuationStatus,
            scaled(totalCostBasis),
            scaled(totalMarketValue),
            scaled(totalUnrealizedGainLoss),
            percentageOrNull(totalUnrealizedGainLoss, totalCostBasis),
            priceAsOf,
            allocations,
            List.copyOf(unpricedHoldings)
        );
    }

    private static boolean isPriced(HoldingValuationInput holding) {
        return holding.currentPrice() != null && holding.priceStatus() != PriceStatus.UNAVAILABLE;
    }

    private static ValuationStatus valuationStatus(int holdingCount, int pricedHoldingCount) {
        if (holdingCount == 0) {
            return ValuationStatus.EMPTY;
        }
        if (pricedHoldingCount == 0) {
            return ValuationStatus.UNAVAILABLE;
        }
        if (pricedHoldingCount == holdingCount) {
            return ValuationStatus.COMPLETE;
        }
        return ValuationStatus.PARTIAL;
    }

    private static List<PortfolioAllocation> allocations(
        List<PricedHolding> pricedHoldings,
        BigDecimal totalMarketValue
    ) {
        if (totalMarketValue.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        return pricedHoldings.stream()
            .map(priced -> new PortfolioAllocation(
                priced.holding().holdingId(),
                priced.holding().ticker(),
                scaled(priced.marketValue()),
                priced.marketValue()
                    .multiply(ONE_HUNDRED)
                    .divide(totalMarketValue, SCALE, RoundingMode.HALF_UP)
            ))
            .toList();
    }

    private static BigDecimal percentageOrNull(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator
            .multiply(ONE_HUNDRED)
            .divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaled(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private record PricedHolding(HoldingValuationInput holding, BigDecimal marketValue) {
    }
}
