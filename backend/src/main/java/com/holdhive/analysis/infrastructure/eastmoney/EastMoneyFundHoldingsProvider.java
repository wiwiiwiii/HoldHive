package com.holdhive.analysis.infrastructure.eastmoney;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdhive.analysis.domain.FundHoldingsLookup;
import com.holdhive.analysis.domain.model.FundConstituent;
import com.holdhive.analysis.domain.model.FundHoldingSnapshot;

/**
 * Live {@link FundHoldingsLookup} backed by East Money's Tiantian Fund public
 * endpoints - the same key-less data source family the project already adopted
 * for price quotes (see docs/guideline/project/market_data_api_zh.md).
 *
 * <p>Two best-effort calls per fund:
 * <ol>
 *   <li>{@code FundMNInverstPosition} - disclosed top stock holdings with NAV
 *       weights, Shenwan sector names and the disclosure cutoff date</li>
 *   <li>{@code FundSearchAPI} - fund display name (falls back to the code)</li>
 * </ol>
 *
 * <p>Every failure mode (unknown fund, timeout, non-JSON body, missing fields)
 * degrades to {@link Optional#empty()} so callers report the fund as
 * {@code FUND_DATA_UNAVAILABLE} instead of failing the whole analysis.
 * Positive results are cached in memory: disclosures change quarterly, so a
 * multi-hour TTL cannot go stale within a demo session.
 *
 * <p>Bond ({@code fundboods}) and FOF ({@code fundfofs}) positions are
 * intentionally ignored: the analysis models equity look-through only, and
 * unpenetrated portions already land in the explicit "基金未穿透部分" bucket.
 */
@Component
@ConditionalOnProperty(name = "holdhive.fund-holdings.provider", havingValue = "eastmoney")
public class EastMoneyFundHoldingsProvider implements FundHoldingsLookup {

    private static final Logger log = LoggerFactory.getLogger(EastMoneyFundHoldingsProvider.class);

    private static final String POSITION_PATH =
            "/FundMNewApi/FundMNInverstPosition?FCODE=%s&deviceid=Wap&plat=Wap&product=EFund&version=2.0.0";
    private static final String NAME_URL = "https://fundsuggest.eastmoney.com/FundSearch/api/FundSearchAPI.ashx?m=1&key=%s";

    private final RestTemplate restTemplate;
    private final FundHoldingsProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public EastMoneyFundHoldingsProvider(RestTemplateBuilder builder, FundHoldingsProperties properties,
                                         ObjectMapper objectMapper) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.timeoutMs()))
                .build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<FundHoldingSnapshot> find(String fundTicker) {
        if (fundTicker == null || fundTicker.isBlank()) {
            return Optional.empty();
        }
        String code = fundTicker.trim();
        CacheEntry cached = cache.get(code);
        if (cached != null
                && cached.fetchedAt().plusSeconds(properties.cacheTtlHours() * 3600).isAfter(Instant.now())) {
            return Optional.of(cached.snapshot());
        }
        Optional<FundHoldingSnapshot> fresh = fetch(code);
        fresh.ifPresent(snapshot -> cache.put(code, new CacheEntry(snapshot, Instant.now())));
        return fresh;
    }

    private Optional<FundHoldingSnapshot> fetch(String code) {
        String body = getUtf8(properties.baseUrl() + String.format(POSITION_PATH, code));
        if (body == null) {
            return Optional.empty();
        }
        return parse(code, body)
                .map(s -> new FundHoldingSnapshot(s.fundTicker(), resolveFundName(code), s.asOfQuarter(),
                        s.constituents()));
    }

    /** Parses the FundMNInverstPosition response; visible for testing. */
    Optional<FundHoldingSnapshot> parse(String code, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("ErrCode").asInt(-1) != 0) {
                return Optional.empty();
            }
            JsonNode stocks = root.path("Datas").path("fundStocks");
            if (!stocks.isArray() || stocks.isEmpty()) {
                return Optional.empty();
            }
            List<FundConstituent> constituents = new ArrayList<>();
            for (JsonNode stock : stocks) {
                String ticker = stock.path("GPDM").asText(null);
                String weight = stock.path("JZBL").asText(null);
                if (ticker == null || ticker.isBlank() || weight == null || weight.isBlank()) {
                    continue;
                }
                try {
                    constituents.add(new FundConstituent(ticker, stock.path("GPJC").asText(ticker),
                            new BigDecimal(weight), stock.path("INDEXNAME").asText(null)));
                } catch (NumberFormatException e) {
                    log.debug("Skipping constituent {} of fund {} with malformed weight {}", ticker, code, weight);
                }
            }
            if (constituents.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new FundHoldingSnapshot(code, code,
                    toQuarterLabel(root.path("Expansion").asText(null)), List.copyOf(constituents)));
        } catch (Exception e) {
            log.warn("Failed to parse fund position response for {}: {}", code, e.getMessage());
            return Optional.empty();
        }
    }

    /** Best-effort fund display name; falls back to the fund code itself. */
    private String resolveFundName(String code) {
        String body = getUtf8(String.format(NAME_URL, code));
        if (body == null) {
            return code;
        }
        try {
            JsonNode datas = objectMapper.readTree(body).path("Datas");
            if (datas.isArray()) {
                for (JsonNode item : datas) {
                    if (code.equals(item.path("CODE").asText())) {
                        String name = item.path("NAME").asText(null);
                        if (name != null && !name.isBlank()) {
                            return name;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse fund name for {}: {}", code, e.getMessage());
        }
        return code;
    }

    /** Converts the disclosure cutoff date "2026-06-30" to a quarter label "2026Q2". */
    static String toQuarterLabel(String expansionDate) {
        if (expansionDate == null || expansionDate.length() < 7) {
            return expansionDate;
        }
        try {
            int year = Integer.parseInt(expansionDate.substring(0, 4));
            int month = Integer.parseInt(expansionDate.substring(5, 7));
            if (month < 1 || month > 12) {
                return expansionDate;
            }
            return year + "Q" + ((month - 1) / 3 + 1);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return expansionDate;
        }
    }

    /**
     * Fetches the response body decoded as UTF-8. The endpoints return JSON
     * without a charset header, so going through {@code byte[]} avoids the
     * RestTemplate default ISO-8859-1 String conversion mangling Chinese names.
     */
    private String getUtf8(String url) {
        try {
            byte[] bytes = restTemplate.getForObject(url, byte[].class);
            return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Fund holdings request failed ({}): {}", url, e.getMessage());
            return null;
        }
    }

    private record CacheEntry(FundHoldingSnapshot snapshot, Instant fetchedAt) {
    }
}
