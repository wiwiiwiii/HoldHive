package com.holdhive.pricing.application;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.holdhive.portfolio.domain.AssetType;

@Service
public class MarketSearchQueryService {

    private static final String SOURCE = "DEMO";
    private static final int RESULT_LIMIT = 10;
    private static final List<MarketSearchItem> DEMO_CATALOG = List.of(
        new MarketSearchItem("AAPL", "Apple Inc.", "NASDAQ", "EASTMONEY", "105.AAPL", AssetType.STOCK),
        new MarketSearchItem("MSFT", "Microsoft Corp.", "NASDAQ", "EASTMONEY", "105.MSFT", AssetType.STOCK),
        new MarketSearchItem("600519", "Kweichow Moutai Co., Ltd.", "SH", "EASTMONEY", "1.600519", AssetType.STOCK),
        new MarketSearchItem("000001", "Ping An Bank Co., Ltd.", "SZ", "EASTMONEY", "0.000001", AssetType.STOCK),
        new MarketSearchItem("VOO", "Vanguard S&P 500 ETF", "NYSE", "EASTMONEY", "105.VOO", AssetType.ETF),
        new MarketSearchItem("SPY", "SPDR S&P 500 ETF Trust", "NYSE", "DEMO", "105.SPY", AssetType.ETF),
        new MarketSearchItem("QQQ", "Invesco QQQ Trust", "NASDAQ", "DEMO", "105.QQQ", AssetType.ETF),
        new MarketSearchItem("FXAIX", "Fidelity 500 Index Fund", "FUND", "DEMO", "MF:FXAIX", AssetType.MUTUAL_FUND),
        new MarketSearchItem("BTC", "Bitcoin", "CRYPTO", "DEMO", "CRYPTO:BTC", AssetType.CRYPTO),
        new MarketSearchItem("ETH", "Ethereum", "CRYPTO", "DEMO", "CRYPTO:ETH", AssetType.CRYPTO),
        new MarketSearchItem("USD", "US Dollar Cash", "CASH", "FIXED", null, AssetType.CASH),
        new MarketSearchItem("USD_DEPOSIT", "USD Bank Deposit", "BANK", "FIXED", null, AssetType.BANK_DEPOSIT)
    );

    public MarketSearchResult search(String query, String market) {
        String displayQuery = trim(query);
        String normalizedQuery = normalize(displayQuery);
        String normalizedMarket = normalize(market);
        if (normalizedQuery.isEmpty()) {
            return new MarketSearchResult("", List.of(), SOURCE, false);
        }

        List<MarketSearchItem> results = DEMO_CATALOG.stream()
            .filter(item -> matchesQuery(item, normalizedQuery))
            .filter(item -> matchesMarket(item, normalizedMarket))
            .limit(RESULT_LIMIT)
            .toList();

        return new MarketSearchResult(displayQuery, results, SOURCE, false);
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
}
