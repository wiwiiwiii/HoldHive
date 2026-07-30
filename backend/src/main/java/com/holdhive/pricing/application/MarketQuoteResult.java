package com.holdhive.pricing.application;

import java.util.List;

import com.holdhive.pricing.domain.MarketQuote;

public record MarketQuoteResult(
    String provider,
    PriceMode priceMode,
    List<MarketQuote> quotes,
    List<UnavailableQuote> unavailable
) {

    public MarketQuoteResult {
        quotes = List.copyOf(quotes);
        unavailable = List.copyOf(unavailable);
    }
}
