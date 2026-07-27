package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.holdhive.common.error.ApiException;
import com.holdhive.portfolio.domain.AssetType;

@Service
public class FundLookthroughService {

    private static final String SOURCE = "DEMO_DISCLOSURE";
    private static final List<String> STANDARD_WARNINGS = List.of(
        "Fund holdings are based on the latest available disclosure and may lag current positions."
    );

    private final Map<Long, FundLookthrough> demoLookthrough = Map.of(
        102L,
        new FundLookthrough(
            102L,
            "VOO",
            "Vanguard S&P 500 ETF",
            AssetType.ETF,
            LocalDate.parse("2026-06-30"),
            SOURCE,
            new BigDecimal("41.15000000"),
            List.of(
                new FundComponent("AAPL", "Apple Inc.", AssetType.STOCK, new BigDecimal("7.12000000")),
                new FundComponent("MSFT", "Microsoft Corp.", AssetType.STOCK, new BigDecimal("6.65000000")),
                new FundComponent("NVDA", "NVIDIA Corp.", AssetType.STOCK, new BigDecimal("6.18000000"))
            ),
            STANDARD_WARNINGS
        ),
        103L,
        new FundLookthrough(
            103L,
            "FXAIX",
            "Fidelity 500 Index Fund",
            AssetType.MUTUAL_FUND,
            LocalDate.parse("2026-06-30"),
            SOURCE,
            new BigDecimal("39.84000000"),
            List.of(
                new FundComponent("AAPL", "Apple Inc.", AssetType.STOCK, new BigDecimal("7.01000000")),
                new FundComponent("MSFT", "Microsoft Corp.", AssetType.STOCK, new BigDecimal("6.54000000")),
                new FundComponent("NVDA", "NVIDIA Corp.", AssetType.STOCK, new BigDecimal("6.07000000"))
            ),
            STANDARD_WARNINGS
        )
    );

    public FundLookthrough getLookthrough(Long fundInstrumentId) {
        FundLookthrough lookthrough = demoLookthrough.get(fundInstrumentId);
        if (lookthrough == null) {
            throw new ApiException(
                HttpStatus.NOT_FOUND,
                "FUND_LOOKTHROUGH_NOT_FOUND",
                "Fund lookthrough not found"
            );
        }
        return lookthrough;
    }
}
