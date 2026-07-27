package com.holdhive.pricing.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.holdhive.pricing.infrastructure.DemoPricingAdapter;
import com.holdhive.pricing.infrastructure.PricingAdapter;

@Configuration
class PricingConfiguration {

    @Bean
    @ConditionalOnMissingBean(PricingAdapter.class)
    PricingAdapter pricingAdapter() {
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
    @ConditionalOnMissingBean(PricingService.class)
    PricingService pricingService(PricingAdapter pricingAdapter) {
        return new PricingService(pricingAdapter);
    }
}
