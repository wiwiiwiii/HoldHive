package com.holdhive.pricing.api.dto;

import java.util.List;

import com.holdhive.pricing.application.MarketSearchResult;

public record MarketSearchResultResponse(
    String query,
    List<MarketSearchItemResponse> results,
    String source,
    boolean cached
) {

    public static MarketSearchResultResponse from(MarketSearchResult result) {
        return new MarketSearchResultResponse(
            result.query(),
            result.results().stream().map(MarketSearchItemResponse::from).toList(),
            result.source(),
            result.cached()
        );
    }
}
