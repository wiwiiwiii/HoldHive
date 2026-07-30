package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.util.List;

import com.holdhive.portfolio.domain.AssetType;

public record PortfolioExposureItem(
    String ticker,
    String displayName,
    AssetType assetType,
    BigDecimal directMarketValue,
    BigDecimal fundLookthroughMarketValue,
    BigDecimal totalExposureValue,
    BigDecimal exposurePercent,
    List<String> sources
) {

    public PortfolioExposureItem {
        sources = List.copyOf(sources);
    }
}
