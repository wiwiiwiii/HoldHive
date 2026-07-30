package com.holdhive.analysis.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.holdhive.analysis.domain.ConcentrationCalculator.RiskLevel;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.FundConstituent;
import com.holdhive.analysis.domain.model.HoldingFact;
import com.holdhive.analysis.domain.support.FundConstituentAttributor;
import com.holdhive.analysis.domain.support.Hhi;
import com.holdhive.analysis.domain.support.PercentMath;

/**
 * L3 layer: look-through effective exposure.
 * <p>
 * Penetrates each FUND holding to its disclosed constituents, aggregates
 * direct stock holdings with indirect fund-driven exposures per ticker,
 * and computes an effective HHI over the resulting "honest" risk buckets.
 * Unattributed fund remainders and non-stock holdings are kept as their own
 * buckets to avoid double-counting or phantom diversification.
 */
public final class LookThroughCalculator {

    public LookThroughResult calculate(List<HoldingFact> holdings, FundHoldingsLookup lookup) {
        BigDecimal total = holdings.stream()
                .map(HoldingFact::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.signum() == 0) {
            return new LookThroughResult(List.of(), BigDecimal.ZERO.setScale(PercentMath.HHI_SCALE, PercentMath.RM),
                    RiskLevel.LOW, null, BigDecimal.ZERO.setScale(PercentMath.PERCENT_SCALE, PercentMath.RM),
                    BigDecimal.ZERO.setScale(PercentMath.PERCENT_SCALE, PercentMath.RM));
        }

        Map<String, BigDecimal> direct = new LinkedHashMap<>();
        for (HoldingFact h : holdings) {
            if (h.assetType() == AssetType.STOCK) {
                direct.merge(h.ticker(), h.marketValue(), BigDecimal::add);
            }
        }

        Map<String, String> names = new LinkedHashMap<>();
        FundConstituentAttributor.Result<String> attribution = FundConstituentAttributor.attribute(
                holdings, lookup, FundConstituent::ticker,
                (c, contrib) -> names.putIfAbsent(c.ticker(), c.name()));
        Map<String, BigDecimal> indirect = attribution.attributedByKey();
        List<BigDecimal> buckets = new ArrayList<>(attribution.unattributedBuckets());

        for (HoldingFact h : holdings) {
            if (h.assetType() == AssetType.STOCK || h.assetType() == AssetType.FUND) continue;
            buckets.add(h.marketValue());
        }

        Set<String> allTickers = new LinkedHashSet<>();
        allTickers.addAll(direct.keySet());
        allTickers.addAll(indirect.keySet());

        List<EffectiveExposure> exposures = new ArrayList<>();
        BigDecimal attributedTotal = BigDecimal.ZERO;

        for (String ticker : allTickers) {
            BigDecimal d = direct.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal ind = indirect.getOrDefault(ticker, BigDecimal.ZERO);
            BigDecimal eff = PercentMath.money(d.add(ind));
            exposures.add(new EffectiveExposure(ticker, names.get(ticker),
                    PercentMath.money(d), PercentMath.money(ind),
                    eff, PercentMath.percentOf(eff, total)));
            buckets.add(eff);
            attributedTotal = attributedTotal.add(eff);
        }

        exposures.sort(Comparator.comparing(EffectiveExposure::effectiveMarketValue).reversed());

        BigDecimal effectiveHhi = Hhi.of(buckets, total);

        EffectiveExposure top = exposures.isEmpty() ? null : exposures.get(0);

        return new LookThroughResult(exposures, effectiveHhi,
                ConcentrationCalculator.riskLevelFor(effectiveHhi),
                top != null ? top.ticker() : null,
                top != null ? top.effectivePercentOfPortfolio() : BigDecimal.ZERO.setScale(PercentMath.PERCENT_SCALE, PercentMath.RM),
                PercentMath.percentOf(attributedTotal, total));
    }

    public record EffectiveExposure(
            String ticker,
            String name,
            BigDecimal directMarketValue,
            BigDecimal indirectMarketValue,
            BigDecimal effectiveMarketValue,
            BigDecimal effectivePercentOfPortfolio) {}

    public record LookThroughResult(
            List<EffectiveExposure> exposures,
            BigDecimal effectiveHhi,
            RiskLevel effectiveRiskLevel,
            String topHoldingTicker,
            BigDecimal topHoldingPercent,
            BigDecimal attributedPercentOfPortfolio) {}
}
