package com.holdhive.pricing.application;

import java.util.List;
import java.util.Objects;

import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.infrastructure.PricingAdapter;

public class PricingService {

    private final PricingAdapter pricingAdapter;

    public PricingService(PricingAdapter pricingAdapter) {
        this.pricingAdapter = Objects.requireNonNull(pricingAdapter, "pricingAdapter must not be null");
    }

    public MarketQuote getQuote(String providerQuoteId) {
        return pricingAdapter.quote(providerQuoteId);
    }

    public List<MarketQuote> getQuotes(List<String> providerQuoteIds) {
        return pricingAdapter.quotes(providerQuoteIds);
    }
}
