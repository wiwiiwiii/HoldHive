package com.holdhive.pricing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarketSearchQueryServiceTest {

    private final MarketSearchQueryService service = new MarketSearchQueryService();

    @Test
    void findsDemoInstrumentsByTickerNameOrProviderQuoteId() {
        MarketSearchResult result = service.search("apple", null);

        assertThat(result.query()).isEqualTo("apple");
        assertThat(result.source()).isEqualTo("DEMO");
        assertThat(result.cached()).isFalse();
        assertThat(result.results())
            .extracting(MarketSearchItem::ticker)
            .contains("AAPL");
    }

    @Test
    void filtersDemoInstrumentsByMarketCode() {
        MarketSearchResult result = service.search("600", "SH");

        assertThat(result.results())
            .hasSize(1)
            .first()
            .satisfies(item -> {
                assertThat(item.ticker()).isEqualTo("600519");
                assertThat(item.exchangeCode()).isEqualTo("SH");
                assertThat(item.providerQuoteId()).isEqualTo("1.600519");
            });
    }

    @Test
    void exposesDemoCatalogItemsForSeededPortfolioAssetTypes() {
        assertThat(service.search("nvda", "US").results())
            .extracting(MarketSearchItem::providerQuoteId)
            .contains("105.NVDA");
        assertThat(service.search("005827", "FUND").results())
            .extracting(MarketSearchItem::assetType)
            .contains(com.holdhive.portfolio.domain.AssetType.MUTUAL_FUND);
        assertThat(service.search("qqq", "US").results())
            .extracting(MarketSearchItem::providerQuoteId)
            .contains("105.QQQ");
        assertThat(service.search("btc", "CRYPTO").results())
            .extracting(MarketSearchItem::providerQuoteId)
            .contains("CRYPTO:BTC");
        assertThat(service.search("deposit", "BANK").results())
            .extracting(MarketSearchItem::ticker)
            .contains("USD_DEPOSIT");
    }

    @Test
    void returnsEmptyResultForBlankQuery() {
        MarketSearchResult result = service.search("   ", "US");

        assertThat(result.query()).isEmpty();
        assertThat(result.results()).isEmpty();
    }
}
