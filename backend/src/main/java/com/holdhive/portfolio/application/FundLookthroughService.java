package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.holdhive.common.error.ApiException;
import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.portfolio.persistence.entity.InstrumentEntity;
import com.holdhive.portfolio.persistence.repository.InstrumentRepository;

@Service
public class FundLookthroughService {

    private static final String SOURCE = "DEMO_DISCLOSURE";
    private static final String UNAVAILABLE_SOURCE = "DISCLOSURE_UNAVAILABLE";
    private static final List<String> STANDARD_WARNINGS = List.of(
        "Fund holdings are based on the latest available disclosure and may lag current positions."
    );
    private final InstrumentRepository instrumentRepository;

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
    private final Map<String, FundLookthrough> demoLookthroughByTicker = demoLookthrough.values().stream()
        .collect(java.util.stream.Collectors.toMap(
            lookthrough -> normalize(lookthrough.ticker()),
            java.util.function.Function.identity(),
            (first, ignored) -> first
        ));

    public FundLookthroughService() {
        this.instrumentRepository = null;
    }

    @Autowired
    public FundLookthroughService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = Objects.requireNonNull(instrumentRepository, "instrumentRepository must not be null");
    }

    public FundLookthrough getLookthrough(Long fundInstrumentId) {
        FundLookthrough lookthrough = demoLookthrough.get(fundInstrumentId);
        if (lookthrough != null) {
            return lookthrough;
        }
        if (instrumentRepository == null) {
            throw notFound();
        }
        InstrumentEntity instrument = instrumentRepository.findById(fundInstrumentId)
            .orElseThrow(FundLookthroughService::notFound);
        return getLookthrough(instrument.getId(), instrument.getTicker(), instrument.getAssetType());
    }

    public FundLookthrough getLookthrough(
        Long fundInstrumentId,
        String ticker,
        AssetType assetType
    ) {
        if (assetType == null || !assetType.isFundLike()) {
            throw notFound();
        }
        return Optional.ofNullable(demoLookthroughByTicker.get(normalize(ticker)))
            .map(lookthrough -> copyForInstrument(fundInstrumentId, lookthrough))
            .orElseGet(() -> unavailableLookthrough(fundInstrumentId, ticker, assetType));
    }

    private static FundLookthrough copyForInstrument(Long fundInstrumentId, FundLookthrough source) {
        return new FundLookthrough(
            fundInstrumentId,
            source.ticker(),
            source.displayName(),
            source.assetType(),
            source.asOfDate(),
            source.source(),
            source.coveragePercent(),
            source.holdings(),
            source.warnings()
        );
    }

    private static FundLookthrough unavailableLookthrough(Long fundInstrumentId, String ticker, AssetType assetType) {
        String normalizedTicker = normalize(ticker);
        return new FundLookthrough(
            fundInstrumentId,
            normalizedTicker,
            normalizedTicker,
            assetType,
            LocalDate.now(),
            UNAVAILABLE_SOURCE,
            BigDecimal.ZERO.setScale(8),
            List.of(),
            List.of("No fund lookthrough disclosure is available for this instrument yet.")
        );
    }

    private static ApiException notFound() {
        return new ApiException(
            HttpStatus.NOT_FOUND,
            "FUND_LOOKTHROUGH_NOT_FOUND",
            "Fund lookthrough not found"
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
