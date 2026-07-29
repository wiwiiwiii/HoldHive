package com.holdhive.pricing.infrastructure;

import java.util.List;

import com.holdhive.pricing.application.MarketSearchItem;

public interface MarketSearchProvider {

    String source();

    List<MarketSearchItem> search(String query);
}
