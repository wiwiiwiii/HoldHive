package com.holdhive.pricing.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

public record MarketQuoteResponse(
    String provider,
    String providerQuoteId,
    String ticker,
    String displayName,
    String currency,
    BigDecimal currentPrice,
    PriceStatus priceStatus,
    Instant priceObservedAt
) {

    public static MarketQuoteResponse from(MarketQuote quote) {
        return new MarketQuoteResponse(
            quote.provider(),
            quote.providerQuoteId(),
            quote.ticker(),
            quote.displayName(),
            quote.currency(),
            quote.currentPrice(),
            quote.priceStatus(),
            quote.priceObservedAt()
        );
    }
}
