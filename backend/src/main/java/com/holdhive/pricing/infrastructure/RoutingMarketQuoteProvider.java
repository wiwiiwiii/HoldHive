package com.holdhive.pricing.infrastructure;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.holdhive.pricing.domain.MarketQuote;

public class RoutingMarketQuoteProvider implements MarketQuoteProvider {

    private final MarketQuoteProvider eastMoneyProvider;
    private final MarketQuoteProvider cryptoProvider;

    public RoutingMarketQuoteProvider(
        MarketQuoteProvider eastMoneyProvider,
        MarketQuoteProvider cryptoProvider
    ) {
        this.eastMoneyProvider = Objects.requireNonNull(eastMoneyProvider, "eastMoneyProvider must not be null");
        this.cryptoProvider = Objects.requireNonNull(cryptoProvider, "cryptoProvider must not be null");
    }

    @Override
    public List<MarketQuote> quotes(List<String> providerQuoteIds) {
        List<String> cryptoIds = providerQuoteIds.stream()
            .filter(RoutingMarketQuoteProvider::isCrypto)
            .toList();
        List<String> marketIds = providerQuoteIds.stream()
            .filter(providerQuoteId -> !isCrypto(providerQuoteId))
            .toList();
        Map<String, MarketQuote> quotesById = java.util.stream.Stream.concat(
                safeQuotes(eastMoneyProvider, marketIds).stream(),
                safeQuotes(cryptoProvider, cryptoIds).stream()
            )
            .collect(Collectors.toMap(
                quote -> normalize(quote.providerQuoteId()),
                Function.identity(),
                (first, ignored) -> first
            ));

        return providerQuoteIds.stream()
            .map(providerQuoteId -> quotesById.get(normalize(providerQuoteId)))
            .filter(Objects::nonNull)
            .toList();
    }

    private static List<MarketQuote> safeQuotes(MarketQuoteProvider provider, List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        try {
            return provider.quotes(ids);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static boolean isCrypto(String providerQuoteId) {
        return normalize(providerQuoteId).startsWith("CRYPTO:");
    }

    private static String normalize(String providerQuoteId) {
        return providerQuoteId == null ? "" : providerQuoteId.trim().toUpperCase(Locale.ROOT);
    }
}
