package com.holdhive.analysis.infrastructure.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdhive.analysis.domain.FundHoldingsLookup;
import com.holdhive.analysis.domain.model.FundHoldingSnapshot;

/**
 * Deterministic {@link FundHoldingsLookup} backed by a fixed JSON fixture
 * ({@code mock/fund-holdings.json}). No network calls - the default provider
 * so tests and demos are repeatable. Set {@code holdhive.fund-holdings.provider=eastmoney}
 * to switch to the live Tiantian Fund implementation instead.
 */
@Component
@ConditionalOnProperty(name = "holdhive.fund-holdings.provider", havingValue = "mock", matchIfMissing = true)
public class MockFundHoldingsProvider implements FundHoldingsLookup {

    private static final String FIXTURE_PATH = "mock/fund-holdings.json";

    private final Map<String, FundHoldingSnapshot> byFundTicker;

    public MockFundHoldingsProvider(ObjectMapper objectMapper) {
        this.byFundTicker = loadFixture(objectMapper);
    }

    @Override
    public Optional<FundHoldingSnapshot> find(String fundTicker) {
        if (fundTicker == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byFundTicker.get(fundTicker.trim()));
    }

    private Map<String, FundHoldingSnapshot> loadFixture(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(FIXTURE_PATH).getInputStream()) {
            List<FundHoldingSnapshot> snapshots = objectMapper.readValue(
                    in, objectMapper.getTypeFactory().constructCollectionType(List.class, FundHoldingSnapshot.class));
            return snapshots.stream()
                    .collect(Collectors.toMap(FundHoldingSnapshot::fundTicker, Function.identity()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock fund holdings fixture: " + FIXTURE_PATH, e);
        }
    }
}
