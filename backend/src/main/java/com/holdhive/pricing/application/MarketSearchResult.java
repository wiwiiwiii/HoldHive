package com.holdhive.pricing.application;

import java.util.List;

public record MarketSearchResult(
    String query,
    List<MarketSearchItem> results,
    String source,
    boolean cached
) {
}
