package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.util.List;

import com.holdhive.pricing.application.PriceMode;

public record PortfolioExposure(
    Long portfolioId,
    String portfolioName,
    String baseCurrency,
    boolean lookthrough,
    PriceMode priceMode,
    BigDecimal totalMarketValue,
    List<PortfolioExposureItem> items,
    List<String> warnings
) {

    public PortfolioExposure {
        items = List.copyOf(items);
        warnings = List.copyOf(warnings);
    }
}
