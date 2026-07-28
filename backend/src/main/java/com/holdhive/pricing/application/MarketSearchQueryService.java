package com.holdhive.pricing.application;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.holdhive.pricing.infrastructure.DemoMarketSearchProvider;
import com.holdhive.pricing.infrastructure.MarketSearchProvider;

@Service
public class MarketSearchQueryService {

    private static final int RESULT_LIMIT = 10;
    private final List<MarketSearchProvider> searchProviders;

    public MarketSearchQueryService() {
        this(List.of(new DemoMarketSearchProvider()));
    }

    @Autowired
    public MarketSearchQueryService(List<MarketSearchProvider> searchProviders) {
        this.searchProviders = searchProviders == null || searchProviders.isEmpty()
            ? List.of(new DemoMarketSearchProvider())
            : List.copyOf(searchProviders);
    }

    public MarketSearchResult search(String query, String market) {
        String displayQuery = trim(query);
        String normalizedQuery = normalize(displayQuery);
        String normalizedMarket = normalize(market);
        if (normalizedQuery.isEmpty()) {
            return new MarketSearchResult("", List.of(), "DEMO", false);
        }

        List<MarketSearchItem> results = searchProviders.stream()
            .flatMap(provider -> safeSearch(provider, displayQuery).stream())
            .filter(item -> matchesQuery(item, normalizedQuery))
            .filter(item -> matchesMarket(item, normalizedMarket))
            .collect(Collectors.toMap(
                MarketSearchQueryService::dedupeKey,
                Function.identity(),
                (first, ignored) -> first,
                java.util.LinkedHashMap::new
            ))
            .values()
            .stream()
            .limit(RESULT_LIMIT)
            .toList();

        String source = searchProviders.size() == 1 ? searchProviders.getFirst().source() : "MIXED";
        return new MarketSearchResult(displayQuery, results, source, false);
    }

    private static List<MarketSearchItem> safeSearch(MarketSearchProvider provider, String query) {
        try {
            return provider.search(query);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static boolean matchesQuery(MarketSearchItem item, String query) {
        return contains(item.ticker(), query)
            || contains(item.displayName(), query)
            || contains(item.providerQuoteId(), query);
    }

    private static boolean matchesMarket(MarketSearchItem item, String market) {
        if (market.isEmpty()) {
            return true;
        }
        return switch (market) {
            case "US" -> "NASDAQ".equals(item.exchangeCode()) || "NYSE".equals(item.exchangeCode());
            case "CN" -> "SH".equals(item.exchangeCode()) || "SZ".equals(item.exchangeCode());
            default -> market.equals(item.exchangeCode());
        };
    }

    private static boolean contains(String value, String query) {
        return value != null && normalize(value).contains(query);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String dedupeKey(MarketSearchItem item) {
        if (item.providerQuoteId() != null && !item.providerQuoteId().isBlank()) {
            return normalize(item.providerQuoteId());
        }
        return normalize(item.assetType() + ":" + item.exchangeCode() + ":" + item.ticker());
    }

}
