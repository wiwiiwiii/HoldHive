package com.holdhive.portfolio.application;

import java.math.BigDecimal;

import com.holdhive.portfolio.domain.AssetType;

public record CreateHoldingCommand(
    AssetType assetType,
    String ticker,
    String exchangeCode,
    String displayName,
    String providerQuoteId,
    String currency,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice
) {
}
