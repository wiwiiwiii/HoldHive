package com.holdhive.pricing.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "holdhive.market")
public class MarketDataProperties {

    private boolean externalEnabled = true;
    private Duration cacheTtl = Duration.ofSeconds(60);
    private Duration httpTimeout = Duration.ofSeconds(3);

    public MarketDataProperties() {
    }

    public MarketDataProperties(
        boolean externalEnabled,
        Duration cacheTtl,
        Duration httpTimeout
    ) {
        this.externalEnabled = externalEnabled;
        this.cacheTtl = cacheTtl;
        this.httpTimeout = httpTimeout;
    }

    public boolean isExternalEnabled() {
        return externalEnabled;
    }

    public void setExternalEnabled(boolean externalEnabled) {
        this.externalEnabled = externalEnabled;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public Duration getHttpTimeout() {
        return httpTimeout;
    }

    public void setHttpTimeout(Duration httpTimeout) {
        this.httpTimeout = httpTimeout;
    }
}
