package com.holdhive.pricing.application;

public record UnavailableQuote(
    String providerQuoteId,
    String reason
) {
}
