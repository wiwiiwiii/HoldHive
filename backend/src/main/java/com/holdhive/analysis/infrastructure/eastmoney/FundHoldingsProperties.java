package com.holdhive.analysis.infrastructure.eastmoney;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code holdhive.fund-holdings.*} keys from application.yml.
 *
 * @param provider      which {@link com.holdhive.analysis.domain.FundHoldingsLookup}
 *                      implementation is active: "mock" (default, deterministic
 *                      fixture) or "eastmoney" (live Tiantian Fund quarterly
 *                      disclosures, no API key required)
 * @param baseUrl       East Money mobile fund API base
 * @param timeoutMs     connect/read timeout in milliseconds for the HTTP calls
 * @param cacheTtlHours how long a fetched snapshot stays in the in-memory cache;
 *                      disclosures only change quarterly, so hours are safe
 */
@ConfigurationProperties(prefix = "holdhive.fund-holdings")
public record FundHoldingsProperties(String provider, String baseUrl, long timeoutMs, long cacheTtlHours) {

    public FundHoldingsProperties {
        if (provider == null || provider.isBlank()) {
            provider = "mock";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://fundmobapi.eastmoney.com";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 3000;
        }
        if (cacheTtlHours <= 0) {
            cacheTtlHours = 24;
        }
    }
}
