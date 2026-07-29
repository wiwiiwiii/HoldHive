package com.holdhive.portfolio.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.domain.PriceStatus;

public record HoldingResponse(
    Long id,
    Long instrumentId,
    String ticker,
    String exchangeCode,
    String displayName,
    AssetType assetType,
    String provider,
    String providerQuoteId,
    String currency,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice,
    BigDecimal currentPrice,
    BigDecimal marketValue,
    BigDecimal costBasis,
    BigDecimal unrealizedGainLoss,
    BigDecimal unrealizedGainLossPercent,
    BigDecimal allocationPercent,
    PriceStatus priceStatus,
    Instant priceObservedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
