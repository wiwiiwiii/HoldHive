package com.holdhive.analysis.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.holdhive.analysis.domain.ConcentrationCalculator.RiskLevel;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.HoldingFact;
import com.holdhive.analysis.domain.support.FundConstituentAttributor;
import com.holdhive.analysis.domain.support.Hhi;
import com.holdhive.analysis.domain.support.PercentMath;

/**
 * Sector (industry) exposure analysis with look-through support.
 * <p>
 * Computes sector-level allocation in two passes:
 * <ol>
 *   <li><b>Direct:</b> map each STOCK holding to its sector via {@link SectorLookup}</li>
 *   <li><b>Look-through:</b> penetrate each FUND holding's disclosed constituents,
 *       attributing their market value to each constituent's sector</li>
 * </ol>
 * Non-equity holdings (CASH, TERM_DEPOSIT, CRYPTO) are reported as their own buckets.
 * Fund remainders (unattributed portions) and stocks with unknown sectors are
 * kept in explicit buckets to avoid phantom diversification — same honest-bucketing
 * philosophy as {@link LookThroughCalculator}.
 * <p>
 * Output: sector allocations, sector HHI, top sector, and attribution coverage.
 */
public final class SectorExposureCalculator {

    public static final String BUCKET_CASH = "Cash & Equivalents";
    public static final String BUCKET_UNKNOWN_SECTOR = "Unknown Sector";
    public static final String BUCKET_FUND_REMAINDER = "Fund Unpenetrated";
    public static final String BUCKET_CRYPTO = "Cryptocurrency";
    public static final String BUCKET_TERM_DEPOSIT = "Term Deposit";

    public SectorExposureResult calculate(List<HoldingFact> holdings,
                                          SectorLookup sectorLookup,
                                          FundHoldingsLookup fundHoldingsLookup) {
        BigDecimal total = holdings.stream()
                .map(HoldingFact::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.signum() == 0) {
            return empty();
        }

        Map<String, BigDecimal> directBySector = new LinkedHashMap<>();
        for (HoldingFact h : holdings) {
            if (h.assetType() == AssetType.STOCK) {
                String sector = sectorLookup.sectorFor(h.ticker())
                        .orElse(BUCKET_UNKNOWN_SECTOR);
                directBySector.merge(sector, h.marketValue(), BigDecimal::add);
            }
        }

        FundConstituentAttributor.Result<String> attribution = FundConstituentAttributor.attribute(
                holdings, fundHoldingsLookup,
                c -> c.sector() != null && !c.sector().isBlank() ? c.sector() : BUCKET_UNKNOWN_SECTOR,
                (c, contrib) -> { },
                fund -> BUCKET_FUND_REMAINDER);
        Map<String, BigDecimal> indirectBySector = attribution.attributedByKey();
        List<BigDecimal> hhiBuckets = new ArrayList<>(attribution.unattributedBuckets());
        attribution.remainderByKey().forEach((sector, remainder) -> directBySector.merge(sector, remainder, BigDecimal::add));

        for (HoldingFact h : holdings) {
            if (h.assetType() == AssetType.STOCK || h.assetType() == AssetType.FUND) continue;
            String bucket = switch (h.assetType()) {
                case CASH -> BUCKET_CASH;
                case CRYPTO -> BUCKET_CRYPTO;
                case TERM_DEPOSIT -> BUCKET_TERM_DEPOSIT;
                default -> h.assetType().name();
            };
            directBySector.merge(bucket, h.marketValue(), BigDecimal::add);
        }

        List<String> allSectors = new ArrayList<>();
        allSectors.addAll(directBySector.keySet());
        for (String s : indirectBySector.keySet()) {
            if (!allSectors.contains(s)) {
                allSectors.add(s);
            }
        }

        List<SectorAllocation> allocations = new ArrayList<>();
        BigDecimal attributedTotal = BigDecimal.ZERO;

        for (String sector : allSectors) {
            BigDecimal direct = PercentMath.money(directBySector.getOrDefault(sector, BigDecimal.ZERO));
            BigDecimal indirect = PercentMath.money(indirectBySector.getOrDefault(sector, BigDecimal.ZERO));
            BigDecimal effective = PercentMath.money(direct.add(indirect));
            allocations.add(new SectorAllocation(sector, direct, indirect, effective,
                    PercentMath.percentOf(effective, total)));
            hhiBuckets.add(effective);
            attributedTotal = attributedTotal.add(effective);
        }

        allocations.sort(Comparator.comparing(SectorAllocation::effectiveMarketValue).reversed());

        BigDecimal sectorHhi = Hhi.of(hhiBuckets, total);

        SectorAllocation top = allocations.isEmpty() ? null : allocations.get(0);

        return new SectorExposureResult(allocations, sectorHhi,
                ConcentrationCalculator.riskLevelFor(sectorHhi),
                top != null ? top.sector() : null,
                top != null ? top.effectivePercentOfPortfolio() : BigDecimal.ZERO.setScale(PercentMath.PERCENT_SCALE, PercentMath.RM),
                PercentMath.percentOf(attributedTotal, total));
    }

    private SectorExposureResult empty() {
        return new SectorExposureResult(List.of(),
                BigDecimal.ZERO.setScale(PercentMath.HHI_SCALE, PercentMath.RM), RiskLevel.LOW,
                null, BigDecimal.ZERO.setScale(PercentMath.PERCENT_SCALE, PercentMath.RM),
                BigDecimal.ZERO.setScale(PercentMath.PERCENT_SCALE, PercentMath.RM));
    }

    public record SectorAllocation(
            String sector,
            BigDecimal directMarketValue,
            BigDecimal indirectMarketValue,
            BigDecimal effectiveMarketValue,
            BigDecimal effectivePercentOfPortfolio) {}

    public record SectorExposureResult(
            List<SectorAllocation> sectors,
            BigDecimal sectorHhi,
            RiskLevel sectorRiskLevel,
            String topSector,
            BigDecimal topSectorPercent,
            BigDecimal attributedPercentOfPortfolio) {}
}
