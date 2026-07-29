package com.holdhive.analysis.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.FundConstituent;
import com.holdhive.analysis.domain.model.FundHoldingSnapshot;
import com.holdhive.analysis.domain.model.HoldingFact;
import com.holdhive.analysis.domain.support.PercentMath;

/**
 * L2 layer: intersects each FUND holding's disclosed top constituents with
 * the portfolio's own STOCK holdings, by ticker. This is the "hidden
 * concentration" check - a portfolio can look diversified by asset type
 * while several funds (and the investor's direct stock picks) are all
 * quietly betting on the same handful of names.
 *
 * <p>Only funds present in {@link FundHoldingsLookup} are analyzed; funds
 * with no disclosure data are reported separately so the caller (and the
 * LLM narrative layer) can be explicit about the gap instead of silently
 * under-counting overlap.
 */
public final class FundOverlapCalculator {

    public FundOverlapResult calculate(List<HoldingFact> holdings, FundHoldingsLookup lookup) {
        BigDecimal total = holdings.stream()
                .map(HoldingFact::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, HoldingFact> stockHoldingsByTicker = holdings.stream()
                .filter(h -> h.assetType() == AssetType.STOCK)
                .collect(Collectors.toMap(HoldingFact::ticker, Function.identity(), (a, b) -> a));

        List<FundOverlapEntry> entries = new ArrayList<>();
        List<UnavailableFund> unavailable = new ArrayList<>();
        Set<String> overlappingTickers = new LinkedHashSet<>();

        List<HoldingFact> fundHoldings = holdings.stream()
                .filter(h -> h.assetType() == AssetType.FUND)
                .toList();

        for (HoldingFact fund : fundHoldings) {
            Optional<FundHoldingSnapshot> snapshot = lookup.find(fund.ticker());
            if (snapshot.isEmpty()) {
                unavailable.add(new UnavailableFund(fund.ticker(), "FUND_DATA_UNAVAILABLE"));
                continue;
            }

            List<OverlapStock> overlaps = new ArrayList<>();
            BigDecimal fundOverlapMarketValue = BigDecimal.ZERO;
            for (FundConstituent constituent : snapshot.get().constituents()) {
                HoldingFact matchedStock = stockHoldingsByTicker.get(constituent.ticker());
                if (matchedStock != null) {
                    fundOverlapMarketValue = fundOverlapMarketValue.add(matchedStock.marketValue());
                    overlaps.add(new OverlapStock(constituent.ticker(), constituent.name(), constituent.weightPercent()));
                    overlappingTickers.add(constituent.ticker());
                }
            }

            entries.add(new FundOverlapEntry(
                    fund.ticker(),
                    snapshot.get().fundName(),
                    overlaps,
                    PercentMath.money(fundOverlapMarketValue),
                    PercentMath.percentOf(fundOverlapMarketValue, total)));
        }

        BigDecimal totalOverlapMarketValue = overlappingTickers.stream()
                .map(ticker -> stockHoldingsByTicker.get(ticker).marketValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalOverlapMarketValue = PercentMath.money(totalOverlapMarketValue);

        return new FundOverlapResult(
                entries,
                unavailable,
                totalOverlapMarketValue,
                PercentMath.percentOf(totalOverlapMarketValue, total));
    }

    public record OverlapStock(String ticker, String name, BigDecimal fundWeightPercent) {
    }

    public record FundOverlapEntry(
            String fundTicker,
            String fundName,
            List<OverlapStock> overlapStocks,
            BigDecimal overlapMarketValue,
            BigDecimal overlapPercentOfPortfolio
    ) {
    }

    public record UnavailableFund(String fundTicker, String reason) {
    }

    public record FundOverlapResult(
            List<FundOverlapEntry> funds,
            List<UnavailableFund> unavailableFunds,
            BigDecimal totalOverlapMarketValue,
            BigDecimal totalOverlapPercentOfPortfolio
    ) {
    }
}
