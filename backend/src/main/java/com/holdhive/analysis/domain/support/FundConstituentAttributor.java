package com.holdhive.analysis.domain.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.holdhive.analysis.domain.FundHoldingsLookup;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.FundConstituent;
import com.holdhive.analysis.domain.model.HoldingFact;

/**
 * Walks every FUND holding and attributes its market value down to disclosed
 * constituents, grouped by a caller-supplied key (ticker for look-through,
 * sector for sector exposure). Funds with no lookup data, and the
 * undisclosed remainder of funds that do have data, are returned as
 * standalone "honest" buckets so callers can feed them into an HHI
 * computation without double-counting or phantom diversification.
 *
 * <p>Extracted because {@code LookThroughCalculator} and
 * {@code SectorExposureCalculator} independently implemented the same walk,
 * differing only in the grouping key.
 */
public final class FundConstituentAttributor {

    private FundConstituentAttributor() {
    }

    public record Result<K>(
            Map<K, BigDecimal> attributedByKey,
            Map<K, BigDecimal> remainderByKey,
            List<BigDecimal> unattributedBuckets) {
    }

    /**
     * Overload for callers that don't need remainders grouped by key (the
     * undisclosed remainder of each fund is folded into the anonymous
     * {@code unattributedBuckets}, same as a fund with no lookup data at all).
     */
    public static <K> Result<K> attribute(
            List<HoldingFact> holdings,
            FundHoldingsLookup lookup,
            Function<FundConstituent, K> keyFn,
            BiConsumer<FundConstituent, BigDecimal> onConstituent) {
        return attribute(holdings, lookup, keyFn, onConstituent, null);
    }

    /**
     * @param keyFn          groups each constituent's contribution, e.g. by ticker or sector
     * @param onConstituent  optional side-channel for extra bookkeeping per constituent
     *                       (e.g. collecting display names); receives the constituent and
     *                       its computed contribution
     * @param remainderKeyFn if non-null, groups each fund's undisclosed remainder into
     *                       {@code remainderByKey} under this key instead of dropping it
     *                       into the anonymous {@code unattributedBuckets}
     */
    public static <K> Result<K> attribute(
            List<HoldingFact> holdings,
            FundHoldingsLookup lookup,
            Function<FundConstituent, K> keyFn,
            BiConsumer<FundConstituent, BigDecimal> onConstituent,
            Function<HoldingFact, K> remainderKeyFn) {
        Map<K, BigDecimal> byKey = new LinkedHashMap<>();
        Map<K, BigDecimal> remainderByKey = new LinkedHashMap<>();
        List<BigDecimal> buckets = new ArrayList<>();

        for (HoldingFact h : holdings) {
            if (h.assetType() != AssetType.FUND) {
                continue;
            }
            var snapshot = lookup.find(h.ticker());
            if (snapshot.isEmpty()) {
                buckets.add(h.marketValue());
                continue;
            }

            BigDecimal attributed = BigDecimal.ZERO;
            for (FundConstituent c : snapshot.get().constituents()) {
                BigDecimal contrib = h.marketValue()
                        .multiply(c.weightPercent())
                        .divide(BigDecimal.valueOf(100), PercentMath.MONEY_SCALE, PercentMath.RM);
                byKey.merge(keyFn.apply(c), contrib, BigDecimal::add);
                onConstituent.accept(c, contrib);
                attributed = attributed.add(contrib);
            }

            BigDecimal remainder = h.marketValue().subtract(attributed);
            if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                if (remainderKeyFn != null) {
                    remainderByKey.merge(remainderKeyFn.apply(h), remainder, BigDecimal::add);
                } else {
                    buckets.add(remainder);
                }
            }
        }

        return new Result<>(byKey, remainderByKey, buckets);
    }
}
