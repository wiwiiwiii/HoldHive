package com.holdhive.pricing.infrastructure;

import java.time.Instant;

@FunctionalInterface
public interface MarketClock {

    Instant now();
}
