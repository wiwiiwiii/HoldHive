package com.holdhive.pricing.infrastructure;

import java.net.URI;

@FunctionalInterface
public interface MarketHttpClient {

    String get(URI uri);
}
