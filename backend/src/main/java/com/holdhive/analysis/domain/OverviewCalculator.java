package com.holdhive.analysis.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.HoldingFact;
import com.holdhive.analysis.domain.support.PercentMath;

/**
 * L0 layer: total market value and allocation by {@link AssetType}.
 * Pure function, no Spring/IO dependencies so it is trivial to unit test.
 */
public final class OverviewCalculator {

    public OverviewResult calculate(List<HoldingFact> holdings) {
        BigDecimal totalMarketValue = PercentMath.money(holdings.stream()
                .map(HoldingFact::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Map<AssetType, BigDecimal> byType = new EnumMap<>(AssetType.class);
        for (HoldingFact holding : holdings) {
            byType.merge(holding.assetType(), holding.marketValue(), BigDecimal::add);
        }

        List<AssetAllocation> allocations = byType.entrySet().stream()
                .map(entry -> new AssetAllocation(
                        entry.getKey(),
                        PercentMath.money(entry.getValue()),
                        PercentMath.percentOf(entry.getValue(), totalMarketValue)))
                .sorted(Comparator.comparing(AssetAllocation::marketValue).reversed())
                .toList();

        return new OverviewResult(totalMarketValue, allocations);
    }

    public record OverviewResult(BigDecimal totalMarketValue, List<AssetAllocation> allocations) {
    }

    public record AssetAllocation(AssetType assetType, BigDecimal marketValue, BigDecimal percent) {
    }
}
