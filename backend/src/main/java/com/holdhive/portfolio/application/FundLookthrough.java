package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.holdhive.portfolio.domain.AssetType;

public record FundLookthrough(
    Long fundInstrumentId,
    String ticker,
    String displayName,
    AssetType assetType,
    LocalDate asOfDate,
    String source,
    BigDecimal coveragePercent,
    List<FundComponent> holdings,
    List<String> warnings
) {

    public FundLookthrough {
        holdings = List.copyOf(holdings);
        warnings = List.copyOf(warnings);
    }
}
