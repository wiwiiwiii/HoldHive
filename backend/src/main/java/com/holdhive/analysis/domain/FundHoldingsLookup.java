package com.holdhive.analysis.domain;

import java.util.Optional;

import com.holdhive.analysis.domain.model.FundHoldingSnapshot;

/**
 * Lookup port for a fund's disclosed top holdings. The domain layer depends
 * only on this interface; {@code infrastructure.mock.MockFundHoldingsProvider}
 * is the demo implementation backed by a fixed JSON fixture. A future
 * implementation could call a real quarterly-disclosure data source without
 * any change to {@link FundOverlapCalculator}.
 */
public interface FundHoldingsLookup {

    Optional<FundHoldingSnapshot> find(String fundTicker);
}
