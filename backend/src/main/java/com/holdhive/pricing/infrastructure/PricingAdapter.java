package com.holdhive.pricing.infrastructure;

import java.util.List;

import com.holdhive.pricing.domain.MarketQuote;

public interface PricingAdapter {

    MarketQuote quote(String providerQuoteId);

    List<MarketQuote> quotes(List<String> providerQuoteIds);
}
