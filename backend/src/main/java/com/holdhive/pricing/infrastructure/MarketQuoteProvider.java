package com.holdhive.pricing.infrastructure;

import java.util.List;

import com.holdhive.pricing.domain.MarketQuote;

@FunctionalInterface
public interface MarketQuoteProvider {

    List<MarketQuote> quotes(List<String> providerQuoteIds);

    default MarketQuote quote(String providerQuoteId) {
        return quotes(List.of(providerQuoteId)).stream()
            .findFirst()
            .orElseGet(() -> MarketQuote.unavailable("MIXED", providerQuoteId, providerQuoteId));
    }
}
