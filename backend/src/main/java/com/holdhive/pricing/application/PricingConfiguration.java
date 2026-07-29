package com.holdhive.pricing.application;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.holdhive.portfolio.persistence.repository.InstrumentRepository;
import com.holdhive.portfolio.persistence.repository.PriceSnapshotRepository;
import com.holdhive.pricing.infrastructure.CachedPricingAdapter;
import com.holdhive.pricing.infrastructure.CoinGeckoMarketDataProvider;
import com.holdhive.pricing.infrastructure.DemoPricingAdapter;
import com.holdhive.pricing.infrastructure.DemoMarketSearchProvider;
import com.holdhive.pricing.infrastructure.EastMoneyMarketDataProvider;
import com.holdhive.pricing.infrastructure.JdkMarketHttpClient;
import com.holdhive.pricing.infrastructure.MarketClock;
import com.holdhive.pricing.infrastructure.MarketDataProperties;
import com.holdhive.pricing.infrastructure.MarketHttpClient;
import com.holdhive.pricing.infrastructure.MarketQuoteProvider;
import com.holdhive.pricing.infrastructure.MarketSearchProvider;
import com.holdhive.pricing.infrastructure.PricingAdapter;
import com.holdhive.pricing.infrastructure.RoutingMarketQuoteProvider;

@Configuration
@EnableConfigurationProperties(MarketDataProperties.class)
class PricingConfiguration {

    @Bean
    MarketClock marketClock() {
        return Instant::now;
    }

    @Bean
    MarketHttpClient marketHttpClient(MarketDataProperties properties) {
        Duration timeout = properties.getHttpTimeout() == null
            ? Duration.ofSeconds(3)
            : properties.getHttpTimeout();
        return new JdkMarketHttpClient(timeout);
    }

    @Bean
    EastMoneyMarketDataProvider eastMoneyMarketDataProvider(MarketHttpClient marketHttpClient) {
        return new EastMoneyMarketDataProvider(marketHttpClient);
    }

    @Bean
    CoinGeckoMarketDataProvider coinGeckoMarketDataProvider(MarketHttpClient marketHttpClient) {
        return new CoinGeckoMarketDataProvider(marketHttpClient);
    }

    @Bean
    MarketQuoteProvider liveMarketQuoteProvider(
        EastMoneyMarketDataProvider eastMoneyMarketDataProvider,
        CoinGeckoMarketDataProvider coinGeckoMarketDataProvider
    ) {
        return new RoutingMarketQuoteProvider(eastMoneyMarketDataProvider, coinGeckoMarketDataProvider);
    }

    @Bean
    MarketSearchProvider demoMarketSearchProvider() {
        return new DemoMarketSearchProvider();
    }

    @Bean
    MarketQuoteProvider demoMarketQuoteProvider() {
        return new DemoPricingAdapter(
            Map.of(
                "105.AAPL", new BigDecimal("210.25"),
                "105.MSFT", new BigDecimal("330.00"),
                "105.VOO", new BigDecimal("510.40"),
                "MF:FXAIX", new BigDecimal("205.35"),
                "CRYPTO:BTC", new BigDecimal("67500.00"),
                "CRYPTO:ETH", new BigDecimal("3650.00"),
                "1.600519", new BigDecimal("1680.00"),
                "0.000001", new BigDecimal("12.34")
            ),
            Instant.parse("2026-07-24T08:29:00Z")
        );
    }

    @Bean
    PricingAdapter pricingAdapter(
        @Qualifier("liveMarketQuoteProvider") MarketQuoteProvider liveMarketQuoteProvider,
        @Qualifier("demoMarketQuoteProvider") MarketQuoteProvider demoMarketQuoteProvider,
        InstrumentRepository instrumentRepository,
        PriceSnapshotRepository priceSnapshotRepository,
        MarketDataProperties marketDataProperties,
        MarketClock marketClock
    ) {
        return new CachedPricingAdapter(
            liveMarketQuoteProvider,
            demoMarketQuoteProvider,
            instrumentRepository,
            priceSnapshotRepository,
            marketDataProperties,
            marketClock
        );
    }

    @Bean
    PricingService pricingService(PricingAdapter pricingAdapter) {
        return new PricingService(pricingAdapter);
    }
}
