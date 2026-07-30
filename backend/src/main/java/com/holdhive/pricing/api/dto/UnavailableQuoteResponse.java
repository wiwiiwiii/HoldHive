package com.holdhive.pricing.api.dto;

import com.holdhive.pricing.application.UnavailableQuote;

public record UnavailableQuoteResponse(
    String providerQuoteId,
    String reason
) {

    public static UnavailableQuoteResponse from(UnavailableQuote unavailableQuote) {
        return new UnavailableQuoteResponse(
            unavailableQuote.providerQuoteId(),
            unavailableQuote.reason()
        );
    }
}
