package com.holdhive.analysis.domain.model;

import java.util.List;

/**
 * A fund's disclosed top holdings as of a given reporting quarter.
 *
 * <p>Data can come from a fixed mock fixture ({@code mock/fund-holdings.json})
 * or from East Money's Tiantian Fund public API (see
 * {@code infrastructure.eastmoney.EastMoneyFundHoldingsProvider}).
 * Switch via {@code holdhive.fund-holdings.provider=mock|eastmoney} in
 * application.yml or the equivalent env variable.
 *
 * @param fundTicker   fund code, e.g. "000001"
 * @param fundName     display name, e.g. "华夏成长混合"
 * @param asOfQuarter  reporting period label, e.g. "2024Q1"
 * @param constituents disclosed top holdings, not necessarily the full portfolio
 */
public record FundHoldingSnapshot(
        String fundTicker,
        String fundName,
        String asOfQuarter,
        List<FundConstituent> constituents
) {
}
