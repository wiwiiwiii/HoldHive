package com.holdhive.analysis.infrastructure.eastmoney;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdhive.analysis.domain.model.FundConstituent;
import com.holdhive.analysis.domain.model.FundHoldingSnapshot;

class EastMoneyFundHoldingsProviderTest {

    private static final String POSITION_URL_PART = "FundMNInverstPosition";
    private static final String NAME_URL_PART = "FundSearchAPI";

    private static final String UNKNOWN_FUND_RESPONSE =
            "{\"Datas\":{\"fundStocks\":null,\"fundboods\":null,\"fundfofs\":null},"
                    + "\"ErrCode\":0,\"Success\":true,\"TotalCount\":0,\"Expansion\":null}";

    private static final String NAME_RESPONSE =
            "{\"ErrCode\":0,\"Datas\":[{\"CODE\":\"005827\",\"NAME\":\"易方达蓝筹精选混合\"}]}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FundHoldingsProperties properties =
            new FundHoldingsProperties("eastmoney", "https://fundmobapi.eastmoney.com", 3000, 24);

    private RestTemplate restTemplate;
    private EastMoneyFundHoldingsProvider provider;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.setConnectTimeout(any())).thenReturn(builder);
        when(builder.setReadTimeout(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        provider = new EastMoneyFundHoldingsProvider(builder, properties, objectMapper);
    }

    @Test
    void mapsLiveResponseToSnapshot() {
        stubPosition(fixtureBytes());
        stubName(NAME_RESPONSE.getBytes(StandardCharsets.UTF_8));

        Optional<FundHoldingSnapshot> result = provider.find("005827");

        assertThat(result).isPresent();
        FundHoldingSnapshot snapshot = result.get();
        assertThat(snapshot.fundTicker()).isEqualTo("005827");
        assertThat(snapshot.fundName()).isEqualTo("易方达蓝筹精选混合");
        assertThat(snapshot.asOfQuarter()).isEqualTo("2026Q2");
        assertThat(snapshot.constituents()).hasSize(10);

        FundConstituent first = snapshot.constituents().get(0);
        assertThat(first.ticker()).isEqualTo("00700");
        assertThat(first.name()).isEqualTo("腾讯控股");
        assertThat(first.weightPercent()).isEqualByComparingTo(new BigDecimal("5.72"));
        assertThat(first.sector()).isEqualTo("Media");

        FundConstituent moutai = snapshot.constituents().stream()
                .filter(c -> c.ticker().equals("600519"))
                .findFirst()
                .orElseThrow();
        assertThat(moutai.name()).isEqualTo("贵州茅台");
        assertThat(moutai.weightPercent()).isEqualByComparingTo(new BigDecimal("5.62"));
        assertThat(moutai.sector()).isEqualTo("Food & Beverage");
    }

    @Test
    void returnsEmptyForUnknownFund() {
        stubPosition(UNKNOWN_FUND_RESPONSE.getBytes(StandardCharsets.UTF_8));

        assertThat(provider.find("999999")).isEmpty();
    }

    @Test
    void returnsEmptyWhenHttpCallFails() {
        when(restTemplate.getForObject(contains(POSITION_URL_PART), eq(byte[].class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThat(provider.find("005827")).isEmpty();
    }

    @Test
    void returnsEmptyOnMalformedJson() {
        stubPosition("not json at all".getBytes(StandardCharsets.UTF_8));

        assertThat(provider.find("005827")).isEmpty();
    }

    @Test
    void fallsBackToTickerAsFundNameWhenNameLookupFails() {
        stubPosition(fixtureBytes());
        when(restTemplate.getForObject(contains(NAME_URL_PART), eq(byte[].class)))
                .thenThrow(new RestClientException("timeout"));

        Optional<FundHoldingSnapshot> result = provider.find("005827");

        assertThat(result).isPresent();
        assertThat(result.get().fundName()).isEqualTo("005827");
    }

    @Test
    void cachesSnapshotAcrossCalls() {
        stubPosition(fixtureBytes());
        stubName(NAME_RESPONSE.getBytes(StandardCharsets.UTF_8));

        provider.find("005827");
        provider.find("005827");

        verify(restTemplate, times(1)).getForObject(contains(POSITION_URL_PART), eq(byte[].class));
        verify(restTemplate, times(1)).getForObject(contains(NAME_URL_PART), eq(byte[].class));
    }

    @Test
    void doesNotCacheUnknownFunds() {
        stubPosition(UNKNOWN_FUND_RESPONSE.getBytes(StandardCharsets.UTF_8));

        provider.find("999999");
        provider.find("999999");

        verify(restTemplate, times(2)).getForObject(contains(POSITION_URL_PART), eq(byte[].class));
    }

    @Test
    void convertsExpansionDateToQuarterLabel() {
        assertThat(EastMoneyFundHoldingsProvider.toQuarterLabel("2026-06-30")).isEqualTo("2026Q2");
        assertThat(EastMoneyFundHoldingsProvider.toQuarterLabel("2025-12-31")).isEqualTo("2025Q4");
        assertThat(EastMoneyFundHoldingsProvider.toQuarterLabel("2025-03-31")).isEqualTo("2025Q1");
        assertThat(EastMoneyFundHoldingsProvider.toQuarterLabel(null)).isNull();
        assertThat(EastMoneyFundHoldingsProvider.toQuarterLabel("garbage")).isEqualTo("garbage");
    }

    private void stubPosition(byte[] body) {
        when(restTemplate.getForObject(contains(POSITION_URL_PART), eq(byte[].class))).thenReturn(body);
    }

    private void stubName(byte[] body) {
        when(restTemplate.getForObject(contains(NAME_URL_PART), eq(byte[].class))).thenReturn(body);
    }

    private byte[] fixtureBytes() {
        try (InputStream in = getClass().getResourceAsStream("/mock/eastmoney-fund-position-005827.json")) {
            if (in == null) {
                throw new IllegalStateException("fixture not found on classpath");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("failed to read fixture", e);
        }
    }
}
