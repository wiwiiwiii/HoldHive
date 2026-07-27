package com.holdhive.pricing.infrastructure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.holdhive.pricing.domain.MarketQuote;

public class DemoPricingAdapter implements PricingAdapter {

    private static final int SCALE = 8;
    private final Map<String, BigDecimal> demoPrices;
    private final Instant observedAt;

    public DemoPricingAdapter(Map<String, BigDecimal> demoPrices, Instant observedAt) {
        this.demoPrices = demoPrices.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> normalize(entry.getKey()),
                entry -> scale(entry.getValue())
            ));
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
    }

    @Override
    public MarketQuote quote(String providerQuoteId) {
        String normalizedQuoteId = normalize(providerQuoteId);
        BigDecimal price = demoPrices.get(normalizedQuoteId);
        if (price == null) {
            return MarketQuote.unavailable("DEMO", normalizedQuoteId, normalizedQuoteId);
        }
        return MarketQuote.demo(normalizedQuoteId, normalizedQuoteId, price, observedAt);
    }

    @Override
    public List<MarketQuote> quotes(List<String> providerQuoteIds) {
        return providerQuoteIds.stream()
            .map(this::quote)
            .toList();
    }

    private static String normalize(String providerQuoteId) {
        return providerQuoteId == null ? "" : providerQuoteId.trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
