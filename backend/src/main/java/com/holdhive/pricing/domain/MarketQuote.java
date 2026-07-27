package com.holdhive.pricing.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuote(
    String provider,
    String providerQuoteId,
    String ticker,
    String displayName,
    String currency,
    BigDecimal currentPrice,
    PriceStatus priceStatus,
    Instant priceObservedAt
) {

    public static MarketQuote demo(
        String providerQuoteId,
        String ticker,
        BigDecimal currentPrice,
        Instant priceObservedAt
    ) {
        return new MarketQuote(
            "DEMO",
            providerQuoteId,
            ticker,
            ticker,
            "USD",
            currentPrice,
            PriceStatus.DEMO,
            priceObservedAt
        );
    }

    public static MarketQuote unavailable(String provider, String providerQuoteId, String ticker) {
        return new MarketQuote(
            provider,
            providerQuoteId,
            ticker,
            ticker,
            "USD",
            null,
            PriceStatus.UNAVAILABLE,
            null
        );
    }
}
