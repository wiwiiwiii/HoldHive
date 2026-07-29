package com.holdhive.analysis.domain.model;

import java.math.BigDecimal;

/**
 * One stock inside a fund's disclosed top holdings.
 *
 * @param ticker        stock code, e.g. "600519"
 * @param name          display name, e.g. "贵州茅台"
 * @param weightPercent percentage of the fund's net asset value (0-100)
 */
public record FundConstituent(String ticker, String name, BigDecimal weightPercent, String sector) {
}
