package com.holdhive.pricing.infrastructure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

public class CoinGeckoMarketDataProvider implements MarketQuoteProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int SCALE = 8;
    private static final String SIMPLE_PRICE_URL = "https://api.coingecko.com/api/v3/simple/price";
    private static final Map<String, Coin> SUPPORTED_COINS = Map.of(
        "BTC", new Coin("bitcoin", "Bitcoin"),
        "ETH", new Coin("ethereum", "Ethereum")
    );

    private final MarketHttpClient httpClient;

    public CoinGeckoMarketDataProvider(MarketHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public List<MarketQuote> quotes(List<String> providerQuoteIds) {
        List<String> requestedIds = providerQuoteIds.stream()
            .filter(Objects::nonNull)
            .map(CoinGeckoMarketDataProvider::normalize)
            .filter(providerQuoteId -> !providerQuoteId.isBlank())
            .toList();
        Map<String, MarketQuote> liveByQuoteId = fetchLiveQuotes(requestedIds);
        return requestedIds.stream()
            .map(providerQuoteId -> liveByQuoteId.getOrDefault(
                providerQuoteId,
                MarketQuote.unavailable("COINGECKO", providerQuoteId, ticker(providerQuoteId))
            ))
            .toList();
    }

    private Map<String, MarketQuote> fetchLiveQuotes(List<String> providerQuoteIds) {
        Map<String, Coin> requestedCoinsByTicker = providerQuoteIds.stream()
            .map(CoinGeckoMarketDataProvider::ticker)
            .filter(SUPPORTED_COINS::containsKey)
            .distinct()
            .collect(Collectors.toMap(
                ticker -> ticker,
                SUPPORTED_COINS::get,
                (first, ignored) -> first,
                LinkedHashMap::new
            ));
        if (requestedCoinsByTicker.isEmpty()) {
            return Map.of();
        }

        String coinIds = requestedCoinsByTicker.values().stream()
            .map(Coin::id)
            .collect(Collectors.joining(","));
        URI uri = URI.create(SIMPLE_PRICE_URL
            + "?ids=" + encode(coinIds)
            + "&vs_currencies=usd"
            + "&include_last_updated_at=true");
        try {
            JsonNode root = OBJECT_MAPPER.readTree(httpClient.get(uri));
            Map<String, MarketQuote> quotes = new LinkedHashMap<>();
            requestedCoinsByTicker.forEach((ticker, coin) -> {
                JsonNode node = root.path(coin.id());
                BigDecimal price = decimalOrNull(node.path("usd"));
                if (price == null) {
                    return;
                }
                String providerQuoteId = "CRYPTO:" + ticker;
                quotes.put(providerQuoteId, new MarketQuote(
                    "COINGECKO",
                    providerQuoteId,
                    ticker,
                    coin.displayName(),
                    "USD",
                    price,
                    PriceStatus.LIVE,
                    instantOrNull(node.path("last_updated_at"))
                ));
            });
            return quotes;
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private static BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.asText()).setScale(SCALE, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Instant instantOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            long epochSeconds = Long.parseLong(node.asText());
            return epochSeconds <= 0 ? null : Instant.ofEpochSecond(epochSeconds);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String normalize(String providerQuoteId) {
        return providerQuoteId == null ? "" : providerQuoteId.trim().toUpperCase(Locale.ROOT);
    }

    private static String ticker(String providerQuoteId) {
        String normalized = normalize(providerQuoteId);
        return normalized.startsWith("CRYPTO:") ? normalized.substring("CRYPTO:".length()) : normalized;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record Coin(String id, String displayName) {
    }
}
