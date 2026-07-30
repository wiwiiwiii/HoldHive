package com.holdhive.portfolio.application;

import java.math.BigDecimal;

import com.holdhive.portfolio.domain.AssetType;

public record FundComponent(
    String ticker,
    String displayName,
    AssetType assetType,
    BigDecimal weightPercent
) {
}
