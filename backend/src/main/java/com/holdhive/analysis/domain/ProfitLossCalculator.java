package com.holdhive.analysis.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.HoldingFact;
import com.holdhive.analysis.domain.support.PercentMath;

/**
 * L4 layer: unrealized profit & loss per holding.
 * <p>
 * Computes P&L and percentage return for every holding that has a known
 * cost basis. Holdings without a cost basis are listed separately as
 * {@code missingCostBasisTickers} (data-gap transparency, same philosophy
 * as {@link FundOverlapCalculator}'s unavailable funds).
 */
public final class ProfitLossCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public ProfitLossResult calculate(List<HoldingFact> holdings) {
        List<HoldingPnl> pnls = new ArrayList<>();
        List<String> missingCost = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalMv = BigDecimal.ZERO;

        for (HoldingFact h : holdings) {
            if (h.costBasis() == null) {
                missingCost.add(h.ticker());
                continue;
            }
            BigDecimal pnl = PercentMath.money(h.marketValue().subtract(h.costBasis()));
            BigDecimal pnlPct = null;
            if (h.costBasis().compareTo(BigDecimal.ZERO) > 0) {
                pnlPct = pnl.multiply(ONE_HUNDRED).divide(h.costBasis(), PercentMath.PERCENT_SCALE, PercentMath.RM);
            }
            pnls.add(new HoldingPnl(h.ticker(), h.assetType(), PercentMath.money(h.marketValue()),
                    PercentMath.money(h.costBasis()), pnl, pnlPct));
            totalCost = totalCost.add(h.costBasis());
            totalMv = totalMv.add(h.marketValue());
        }

        pnls.sort(Comparator.comparing(HoldingPnl::unrealizedPnlPercent,
                Comparator.nullsLast(Comparator.reverseOrder())));

        BigDecimal totalPnl = PercentMath.money(totalMv.subtract(totalCost));
        BigDecimal totalPnlPct = null;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalPnlPct = totalPnl.multiply(ONE_HUNDRED).divide(totalCost, PercentMath.PERCENT_SCALE, PercentMath.RM);
        }

        HoldingPnl best = null;
        HoldingPnl worst = null;
        for (HoldingPnl p : pnls) {
            if (p.unrealizedPnlPercent() == null) continue;
            if (best == null || p.unrealizedPnlPercent().compareTo(best.unrealizedPnlPercent()) > 0) best = p;
            if (worst == null || p.unrealizedPnlPercent().compareTo(worst.unrealizedPnlPercent()) < 0) worst = p;
        }

        return new ProfitLossResult(pnls, PercentMath.money(totalCost), PercentMath.money(totalMv),
                totalPnl, totalPnlPct,
                best != null ? best.ticker() : null,
                best != null ? best.unrealizedPnlPercent() : null,
                worst != null ? worst.ticker() : null,
                worst != null ? worst.unrealizedPnlPercent() : null,
                missingCost);
    }

    public record HoldingPnl(
            String ticker,
            AssetType assetType,
            BigDecimal marketValue,
            BigDecimal costBasis,
            BigDecimal unrealizedPnl,
            BigDecimal unrealizedPnlPercent) {}

    public record ProfitLossResult(
            List<HoldingPnl> holdings,
            BigDecimal totalCostBasis,
            BigDecimal totalMarketValue,
            BigDecimal totalUnrealizedPnl,
            BigDecimal totalUnrealizedPnlPercent,
            String bestPerformerTicker,
            BigDecimal bestPerformerPnlPercent,
            String worstPerformerTicker,
            BigDecimal worstPerformerPnlPercent,
            List<String> missingCostBasisTickers) {}
}
