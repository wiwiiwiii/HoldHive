package com.holdhive.pricing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

class CoinGeckoMarketDataProviderTest {

    @Test
    void parsesCryptoSimplePriceResponse() {
        CoinGeckoMarketDataProvider provider = new CoinGeckoMarketDataProvider(uri -> {
            assertThat(uri.toString()).contains("ids=bitcoin%2Cethereum");
            return """
                {
                  "bitcoin": {"usd": 63751, "last_updated_at": 1785259270},
                  "ethereum": {"usd": 1907.82, "last_updated_at": 1785259260}
                }
                """;
        });

        List<MarketQuote> quotes = provider.quotes(List.of("CRYPTO:BTC", "CRYPTO:ETH", "CRYPTO:DOGE"));

        assertThat(quotes).hasSize(3);
        assertThat(quotes.getFirst())
            .satisfies(quote -> {
                assertThat(quote.provider()).isEqualTo("COINGECKO");
                assertThat(quote.providerQuoteId()).isEqualTo("CRYPTO:BTC");
                assertThat(quote.ticker()).isEqualTo("BTC");
                assertThat(quote.displayName()).isEqualTo("Bitcoin");
                assertThat(quote.currentPrice()).isEqualByComparingTo("63751.00000000");
                assertThat(quote.priceStatus()).isEqualTo(PriceStatus.LIVE);
            });
        assertThat(quotes.get(1).currentPrice()).isEqualByComparingTo("1907.82000000");
        assertThat(quotes.get(2).priceStatus()).isEqualTo(PriceStatus.UNAVAILABLE);
    }
}
