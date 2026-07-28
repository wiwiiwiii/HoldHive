package com.holdhive.portfolio.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.holdhive.common.error.ApiException;
import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.portfolio.persistence.entity.HoldingEntity;
import com.holdhive.portfolio.persistence.entity.InstrumentEntity;
import com.holdhive.portfolio.persistence.entity.PortfolioEntity;
import com.holdhive.portfolio.persistence.repository.HoldingRepository;
import com.holdhive.portfolio.persistence.repository.InstrumentRepository;
import com.holdhive.portfolio.persistence.repository.PortfolioRepository;

@Service
public class HoldingCommandService {

    private static final BigDecimal FIXED_PRICE = BigDecimal.ONE.setScale(8, RoundingMode.HALF_UP);

    private final PortfolioRepository portfolioRepository;
    private final InstrumentRepository instrumentRepository;
    private final HoldingRepository holdingRepository;

    public HoldingCommandService(
        PortfolioRepository portfolioRepository,
        InstrumentRepository instrumentRepository,
        HoldingRepository holdingRepository
    ) {
        this.portfolioRepository = portfolioRepository;
        this.instrumentRepository = instrumentRepository;
        this.holdingRepository = holdingRepository;
    }

    @Transactional
    public Long createHolding(CreateHoldingCommand command) {
        PortfolioEntity portfolio = defaultPortfolio();
        NormalizedHolding normalized = normalize(command);
        InstrumentEntity instrument = instrumentRepository
            .findByAssetTypeAndTickerAndExchangeCode(
                normalized.assetType(),
                normalized.ticker(),
                normalized.exchangeCode()
            )
            .orElseGet(() -> instrumentRepository.saveAndFlush(new InstrumentEntity(
                normalized.assetType(),
                normalized.ticker(),
                normalized.exchangeCode(),
                normalized.displayName(),
                normalized.provider(),
                normalized.providerQuoteId(),
                normalized.currency()
            )));

        if (holdingRepository.existsByPortfolioIdAndInstrumentId(portfolio.getId(), instrument.getId())) {
            throw duplicateHolding(normalized.ticker());
        }

        try {
            HoldingEntity holding = holdingRepository.saveAndFlush(new HoldingEntity(
                portfolio,
                instrument,
                scale(normalized.quantity()),
                scale(normalized.averagePurchasePrice())
            ));
            return holding.getId();
        } catch (DataIntegrityViolationException exception) {
            throw duplicateHolding(normalized.ticker());
        }
    }

    @Transactional
    public void deleteHolding(Long holdingId) {
        HoldingEntity holding = holdingRepository.findById(holdingId)
            .orElseThrow(() -> notFound(holdingId));
        holdingRepository.delete(holding);
        holdingRepository.flush();
    }

    private PortfolioEntity defaultPortfolio() {
        return portfolioRepository.findFirstByOrderByIdAsc()
            .orElseGet(() -> portfolioRepository.saveAndFlush(new PortfolioEntity("My Portfolio", "USD")));
    }

    private static NormalizedHolding normalize(CreateHoldingCommand command) {
        AssetType assetType = command.assetType();
        String ticker = requiredUpper(command.ticker());
        String exchangeCode = defaultExchangeCode(assetType, command.exchangeCode());
        String providerQuoteId = blankToNull(command.providerQuoteId());
        if (providerQuoteId != null) {
            providerQuoteId = providerQuoteId.toUpperCase(Locale.ROOT);
        }
        String currency = defaultCurrency(command.currency());
        String provider = defaultProvider(assetType, providerQuoteId);
        BigDecimal averagePurchasePrice = assetType.isFixedValueAsset()
            ? FIXED_PRICE
            : command.averagePurchasePrice();

        return new NormalizedHolding(
            assetType,
            ticker,
            exchangeCode,
            blankToNull(command.displayName()),
            provider,
            providerQuoteId,
            currency,
            command.quantity(),
            averagePurchasePrice
        );
    }

    private static String requiredUpper(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String defaultExchangeCode(AssetType assetType, String exchangeCode) {
        String normalized = blankToNull(exchangeCode);
        if (normalized != null) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return switch (assetType) {
            case CASH -> "CASH";
            case BANK_DEPOSIT -> "BANK";
            default -> "UNKNOWN";
        };
    }

    private static String defaultCurrency(String currency) {
        String normalized = blankToNull(currency);
        return normalized == null ? "USD" : normalized.toUpperCase(Locale.ROOT);
    }

    private static String defaultProvider(AssetType assetType, String providerQuoteId) {
        if (assetType.isFixedValueAsset()) {
            return "FIXED";
        }
        if (providerQuoteId == null) {
            return null;
        }
        if (providerQuoteId.startsWith("CRYPTO:") || providerQuoteId.startsWith("MF:")) {
            return "DEMO";
        }
        if (providerQuoteId.matches("\\d+\\..+")) {
            return "EASTMONEY";
        }
        return "DEMO";
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(8, RoundingMode.HALF_UP);
    }

    private static ApiException duplicateHolding(String ticker) {
        return new ApiException(
            HttpStatus.CONFLICT,
            "HOLDING_ALREADY_EXISTS",
            ticker + " already exists in the default portfolio"
        );
    }

    private static ApiException notFound(Long holdingId) {
        return new ApiException(
            HttpStatus.NOT_FOUND,
            "HOLDING_NOT_FOUND",
            "Holding " + holdingId + " was not found"
        );
    }

    private record NormalizedHolding(
        AssetType assetType,
        String ticker,
        String exchangeCode,
        String displayName,
        String provider,
        String providerQuoteId,
        String currency,
        BigDecimal quantity,
        BigDecimal averagePurchasePrice
    ) {
    }
}
