package com.holdhive.analysis.domain;

import java.util.Optional;

/**
 * Lookup port for a stock's industry/sector classification.
 * Domain layer depends only on this interface; infrastructure provides
 * the actual data source (mock JSON fixture in demo, real data source
 * such as Shenwan/GICS classification in production).
 */
public interface SectorLookup {

    Optional<String> sectorFor(String stockTicker);
}
