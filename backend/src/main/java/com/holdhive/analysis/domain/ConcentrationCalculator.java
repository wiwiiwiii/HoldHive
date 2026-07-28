package com.holdhive.analysis.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.HoldingFact;
import com.holdhive.analysis.domain.support.Hhi;
import com.holdhive.analysis.domain.support.PercentMath;

/**
 * L1 layer: concentration risk expressed as the Herfindahl-Hirschman Index
 * (HHI = sum of squared weights, each weight in [0,1]) plus headline facts:
 * the single largest holding and the top-N holdings with their combined
 * weight. The top-N facts exist so downstream narrators (L5 LLM) can quote
 * "top-N combined" figures verbatim instead of doing arithmetic themselves.
 * Kept to one number + headline facts on purpose -
 * this is a demo, not a full risk engine (no VaR/Sharpe/volatility).
 */
public final class ConcentrationCalculator {

    private static final int TOP_N = 5;
    private static final BigDecimal LOW_THRESHOLD = new BigDecimal("0.15");
    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("0.25");

    public ConcentrationResult calculate(List<HoldingFact> holdings) {
        BigDecimal total = holdings.stream()
                .map(HoldingFact::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.signum() == 0 || holdings.isEmpty()) {
            return new ConcentrationResult(
                    BigDecimal.ZERO,
                    null,
                    BigDecimal.ZERO.setScale(PercentMath.PERCENT_SCALE, PercentMath.RM),
                    RiskLevel.LOW,
                    holdings.size(),
                    List.of(),
                    BigDecimal.ZERO.setScale(PercentMath.PERCENT_SCALE, PercentMath.RM));
        }

        BigDecimal hhi = Hhi.of(holdings.stream().map(HoldingFact::marketValue).toList(), total);

        List<HoldingFact> sorted = holdings.stream()
                .sorted(Comparator.comparing(HoldingFact::marketValue).reversed())
                .toList();

        HoldingFact top = sorted.get(0);
        BigDecimal topPercent = PercentMath.percentOf(top.marketValue(), total);

        List<TopHolding> topHoldings = new ArrayList<>();
        BigDecimal topNMarketValue = BigDecimal.ZERO;
        for (HoldingFact h : sorted.stream().limit(TOP_N).toList()) {
            topHoldings.add(new TopHolding(h.ticker(), h.assetType(), PercentMath.percentOf(h.marketValue(), total)));
            topNMarketValue = topNMarketValue.add(h.marketValue());
        }

        return new ConcentrationResult(hhi, top.ticker(), topPercent, riskLevelFor(hhi),
                holdings.size(), List.copyOf(topHoldings), PercentMath.percentOf(topNMarketValue, total));
    }

    static RiskLevel riskLevelFor(BigDecimal hhi) {
        if (hhi.compareTo(LOW_THRESHOLD) < 0) {
            return RiskLevel.LOW;
        }
        if (hhi.compareTo(MEDIUM_THRESHOLD) <= 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.HIGH;
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }

    public record TopHolding(
            String ticker,
            AssetType assetType,
            BigDecimal percentOfPortfolio) {}

    public record ConcentrationResult(
            BigDecimal hhi,
            String topHoldingTicker,
            BigDecimal topHoldingPercent,
            RiskLevel riskLevel,
            int holdingCount,
            List<TopHolding> topHoldings,
            BigDecimal topHoldingsCombinedPercent
    ) {
    }
}
