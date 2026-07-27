package com.holdhive.portfolio.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.holdhive.portfolio.domain.PortfolioCalculator;

@Configuration
class PortfolioApplicationConfiguration {

    @Bean
    @ConditionalOnMissingBean(PortfolioCalculator.class)
    PortfolioCalculator portfolioCalculator() {
        return new PortfolioCalculator();
    }

    @Bean
    @ConditionalOnMissingBean(PortfolioHoldingReader.class)
    PortfolioHoldingReader portfolioHoldingReader() {
        return new EmptyPortfolioHoldingReader();
    }
}
