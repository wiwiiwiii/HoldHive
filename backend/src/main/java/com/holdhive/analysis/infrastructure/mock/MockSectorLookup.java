package com.holdhive.analysis.infrastructure.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdhive.analysis.domain.SectorLookup;

/**
 * Demo-only {@link SectorLookup} backed by a fixed JSON fixture
 * ({@code mock/stock-sectors.json}). Maps stock tickers to industry sectors
 * using Shenwan-style classification names.
 */
@Component
public class MockSectorLookup implements SectorLookup {

    private static final String FIXTURE_PATH = "mock/stock-sectors.json";

    private final Map<String, String> sectorByTicker;

    @SuppressWarnings("unchecked")
    public MockSectorLookup(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(FIXTURE_PATH).getInputStream()) {
            this.sectorByTicker = objectMapper.readValue(in, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock sector fixture: " + FIXTURE_PATH, e);
        }
    }

    @Override
    public Optional<String> sectorFor(String stockTicker) {
        if (stockTicker == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sectorByTicker.get(stockTicker.trim()));
    }
}
