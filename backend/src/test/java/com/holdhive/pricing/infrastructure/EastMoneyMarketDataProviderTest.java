package com.holdhive.pricing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.MarketSearchItem;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

class EastMoneyMarketDataProviderTest {

    @Test
    void parsesLiveBatchQuotesFromEastMoneyResponse() {
        EastMoneyMarketDataProvider provider = new EastMoneyMarketDataProvider(uri -> """
            {
              "rc": 0,
              "data": {
                "diff": [
                  {"f2": 339.095, "f12": "AAPL", "f13": 105, "f14": "苹果", "f124": 1785259364},
                  {"f2": "-", "f12": "BAD", "f13": 105, "f14": "Bad Quote", "f124": "-"}
                ]
              }
            }
            """);

        List<MarketQuote> quotes = provider.quotes(List.of("105.AAPL", "105.BAD"));

        assertThat(quotes).hasSize(2);
        assertThat(quotes.getFirst())
            .satisfies(quote -> {
                assertThat(quote.provider()).isEqualTo("EASTMONEY");
                assertThat(quote.providerQuoteId()).isEqualTo("105.AAPL");
                assertThat(quote.ticker()).isEqualTo("AAPL");
                assertThat(quote.displayName()).isEqualTo("苹果");
                assertThat(quote.currentPrice()).isEqualByComparingTo("339.09500000");
                assertThat(quote.priceStatus()).isEqualTo(PriceStatus.LIVE);
                assertThat(quote.priceObservedAt()).isNotNull();
            });
        assertThat(quotes.get(1).priceStatus()).isEqualTo(PriceStatus.UNAVAILABLE);
    }

    @Test
    void parsesSearchSuggestionsAndMapsAssetType() {
        EastMoneyMarketDataProvider provider = new EastMoneyMarketDataProvider(uri -> {
            assertThat(uri.toString()).contains("input=AAPL");
            return """
                {
                  "QuotationCodeTable": {
                    "Data": [
                      {
                        "Code": "AAPL",
                        "Name": "苹果",
                        "JYS": "NASDAQ",
                        "QuoteID": "105.AAPL",
                        "SecurityTypeName": "美股"
                      }
                    ]
                  }
                }
                """;
        });

        List<MarketSearchItem> results = provider.search("AAPL");

        assertThat(results).singleElement()
            .satisfies(item -> {
                assertThat(item.ticker()).isEqualTo("AAPL");
                assertThat(item.displayName()).isEqualTo("苹果");
                assertThat(item.exchangeCode()).isEqualTo("NASDAQ");
                assertThat(item.provider()).isEqualTo("EASTMONEY");
                assertThat(item.providerQuoteId()).isEqualTo("105.AAPL");
                assertThat(item.assetType()).isEqualTo(AssetType.STOCK);
            });
    }

    @FunctionalInterface
    private interface TestHttpClient extends MarketHttpClient {
        @Override
        String get(URI uri);
    }
}
