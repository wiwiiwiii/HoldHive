package com.holdhive.pricing.api.dto;

import java.util.List;

import com.holdhive.pricing.application.MarketQuoteResult;
import com.holdhive.pricing.application.PriceMode;

public record MarketQuoteResultResponse(
    String provider,
    PriceMode priceMode,
    List<MarketQuoteResponse> quotes,
    List<UnavailableQuoteResponse> unavailable
) {

    public static MarketQuoteResultResponse from(MarketQuoteResult result) {
        return new MarketQuoteResultResponse(
            result.provider(),
            result.priceMode(),
            result.quotes().stream().map(MarketQuoteResponse::from).toList(),
            result.unavailable().stream().map(UnavailableQuoteResponse::from).toList()
        );
    }
}
