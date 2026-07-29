package com.holdhive.pricing.infrastructure;

import java.util.List;
import java.util.Locale;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.MarketSearchItem;

public class DemoMarketSearchProvider implements MarketSearchProvider {

    private static final List<MarketSearchItem> DEMO_CATALOG = List.of(
        new MarketSearchItem("AAPL", "Apple Inc.", "NASDAQ", "EASTMONEY", "105.AAPL", AssetType.STOCK),
        new MarketSearchItem("MSFT", "Microsoft Corp.", "NASDAQ", "EASTMONEY", "105.MSFT", AssetType.STOCK),
        new MarketSearchItem("600519", "Kweichow Moutai Co., Ltd.", "SH", "EASTMONEY", "1.600519", AssetType.STOCK),
        new MarketSearchItem("000001", "Ping An Bank Co., Ltd.", "SZ", "EASTMONEY", "0.000001", AssetType.STOCK),
        new MarketSearchItem("VOO", "Vanguard S&P 500 ETF", "NYSE", "EASTMONEY", "105.VOO", AssetType.ETF),
        new MarketSearchItem("SPY", "SPDR S&P 500 ETF Trust", "NYSE", "DEMO", "105.SPY", AssetType.ETF),
        new MarketSearchItem("QQQ", "Invesco QQQ Trust", "NASDAQ", "DEMO", "105.QQQ", AssetType.ETF),
        new MarketSearchItem("FXAIX", "Fidelity 500 Index Fund", "FUND", "DEMO", "MF:FXAIX", AssetType.MUTUAL_FUND),
        new MarketSearchItem("BTC", "Bitcoin", "CRYPTO", "COINGECKO", "CRYPTO:BTC", AssetType.CRYPTO),
        new MarketSearchItem("ETH", "Ethereum", "CRYPTO", "COINGECKO", "CRYPTO:ETH", AssetType.CRYPTO),
        new MarketSearchItem("USD", "US Dollar Cash", "CASH", "FIXED", null, AssetType.CASH),
        new MarketSearchItem("USD_DEPOSIT", "USD Bank Deposit", "BANK", "FIXED", null, AssetType.BANK_DEPOSIT)
    );

    @Override
    public String source() {
        return "DEMO";
    }

    @Override
    public List<MarketSearchItem> search(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        return DEMO_CATALOG.stream()
            .filter(item -> contains(item.ticker(), normalizedQuery)
                || contains(item.displayName(), normalizedQuery)
                || contains(item.providerQuoteId(), normalizedQuery))
            .toList();
    }

    private static boolean contains(String value, String query) {
        return value != null && normalize(value).contains(query);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
