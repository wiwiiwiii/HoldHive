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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.MarketSearchItem;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

public class EastMoneyMarketDataProvider implements MarketQuoteProvider, MarketSearchProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int SCALE = 8;
    private static final String QUOTE_URL = "https://push2.eastmoney.com/api/qt/ulist.np/get";
    private static final String SEARCH_URL = "https://searchapi.eastmoney.com/api/suggest/get";
    private static final String SEARCH_TOKEN = "D43BF722C8E33BD5A6040B1F8FD653E5";
    private static final String QUOTE_FIELDS = "f12,f14,f2,f13,f124";

    private final MarketHttpClient httpClient;

    public EastMoneyMarketDataProvider(MarketHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public String source() {
        return "EASTMONEY";
    }

    @Override
    public List<MarketQuote> quotes(List<String> providerQuoteIds) {
        List<String> requestedIds = providerQuoteIds.stream()
            .filter(Objects::nonNull)
            .map(EastMoneyMarketDataProvider::normalize)
            .filter(providerQuoteId -> !providerQuoteId.isBlank())
            .toList();
        if (requestedIds.isEmpty()) {
            return List.of();
        }

        Map<String, MarketQuote> liveById = fetchLiveQuotes(requestedIds);
        return requestedIds.stream()
            .map(providerQuoteId -> liveById.getOrDefault(
                providerQuoteId,
                MarketQuote.unavailable("EASTMONEY", providerQuoteId, tickerFromProviderQuoteId(providerQuoteId))
            ))
            .toList();
    }

    @Override
    public List<MarketSearchItem> search(String query) {
        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.isBlank()) {
            return List.of();
        }
        URI uri = URI.create(SEARCH_URL
            + "?input=" + encode(trimmedQuery)
            + "&type=14"
            + "&token=" + SEARCH_TOKEN);
        try {
            JsonNode root = OBJECT_MAPPER.readTree(httpClient.get(uri));
            JsonNode data = root.path("QuotationCodeTable").path("Data");
            if (!data.isArray()) {
                return List.of();
            }
            java.util.ArrayList<MarketSearchItem> results = new java.util.ArrayList<>();
            for (JsonNode item : data) {
                String ticker = item.path("Code").asText("");
                String quoteId = item.path("QuoteID").asText("");
                if (ticker.isBlank() || quoteId.isBlank()) {
                    continue;
                }
                results.add(new MarketSearchItem(
                    ticker.toUpperCase(Locale.ROOT),
                    textOrDefault(item.path("Name"), ticker),
                    exchangeCode(item),
                    "EASTMONEY",
                    quoteId.toUpperCase(Locale.ROOT),
                    assetType(item)
                ));
            }
            return results;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<String, MarketQuote> fetchLiveQuotes(List<String> providerQuoteIds) {
        URI uri = URI.create(QUOTE_URL
            + "?fltt=2&invt=2&fields=" + encode(QUOTE_FIELDS)
            + "&secids=" + encode(String.join(",", providerQuoteIds)));
        try {
            JsonNode root = OBJECT_MAPPER.readTree(httpClient.get(uri));
            JsonNode diff = root.path("data").path("diff");
            if (!diff.isArray()) {
                return Map.of();
            }
            Map<String, MarketQuote> quotes = new LinkedHashMap<>();
            for (JsonNode item : diff) {
                String ticker = item.path("f12").asText("");
                String marketCode = item.path("f13").asText("");
                String providerQuoteId = normalize(marketCode + "." + ticker);
                quotes.put(providerQuoteId, quoteFromDiff(providerQuoteId, item));
            }
            return quotes;
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private static MarketQuote quoteFromDiff(String providerQuoteId, JsonNode item) {
        String ticker = item.path("f12").asText(tickerFromProviderQuoteId(providerQuoteId));
        BigDecimal currentPrice = decimalOrNull(item.path("f2"));
        if (currentPrice == null) {
            return MarketQuote.unavailable("EASTMONEY", providerQuoteId, ticker);
        }
        return new MarketQuote(
            "EASTMONEY",
            providerQuoteId,
            ticker,
            textOrDefault(item.path("f14"), ticker),
            "USD",
            currentPrice,
            PriceStatus.LIVE,
            instantOrNull(item.path("f124"))
        );
    }

    private static BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual() && (node.asText().isBlank() || "-".equals(node.asText()))) {
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

    private static String textOrDefault(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return fallback;
        }
        return node.asText();
    }

    private static String exchangeCode(JsonNode item) {
        String jys = item.path("JYS").asText("");
        if (!jys.isBlank()) {
            return jys.toUpperCase(Locale.ROOT);
        }
        return switch (item.path("MktNum").asText("")) {
            case "1" -> "SH";
            case "0" -> "SZ";
            case "105" -> "NASDAQ";
            case "106" -> "NYSE";
            case "116" -> "HK";
            default -> "UNKNOWN";
        };
    }

    private static AssetType assetType(JsonNode item) {
        String securityTypeName = item.path("SecurityTypeName").asText("").toUpperCase(Locale.ROOT);
        if (securityTypeName.contains("ETF")) {
            return AssetType.ETF;
        }
        if (securityTypeName.contains("基金")) {
            return AssetType.MUTUAL_FUND;
        }
        return AssetType.STOCK;
    }

    private static String normalize(String providerQuoteId) {
        return providerQuoteId == null ? "" : providerQuoteId.trim().toUpperCase(Locale.ROOT);
    }

    private static String tickerFromProviderQuoteId(String providerQuoteId) {
        String normalized = normalize(providerQuoteId);
        int dotIndex = normalized.indexOf('.');
        return dotIndex >= 0 && dotIndex + 1 < normalized.length()
            ? normalized.substring(dotIndex + 1)
            : normalized;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
