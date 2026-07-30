package com.holdhive.portfolio.persistence;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.holdhive.portfolio.application.HoldingPosition;
import com.holdhive.portfolio.application.PortfolioHoldingReader;
import com.holdhive.portfolio.application.PortfolioSnapshot;
import com.holdhive.portfolio.persistence.entity.HoldingEntity;
import com.holdhive.portfolio.persistence.entity.InstrumentEntity;
import com.holdhive.portfolio.persistence.entity.PortfolioEntity;
import com.holdhive.portfolio.persistence.repository.HoldingRepository;
import com.holdhive.portfolio.persistence.repository.PortfolioRepository;

@Component
class JpaPortfolioHoldingReader implements PortfolioHoldingReader {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;

    JpaPortfolioHoldingReader(
        PortfolioRepository portfolioRepository,
        HoldingRepository holdingRepository
    ) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioSnapshot findDefaultPortfolio() {
        PortfolioEntity portfolio = portfolioRepository.findFirstByOrderByIdAsc()
            .orElseGet(() -> new PortfolioEntity("My Portfolio", "USD"));
        if (portfolio.getId() == null) {
            return new PortfolioSnapshot(1L, portfolio.getName(), portfolio.getBaseCurrency(), List.of());
        }

        List<HoldingPosition> holdings = holdingRepository.findByPortfolioId(portfolio.getId()).stream()
            .sorted(Comparator.comparing(holding -> holding.getInstrument().getTicker()))
            .map(this::toPosition)
            .toList();

        return new PortfolioSnapshot(
            portfolio.getId(),
            portfolio.getName(),
            portfolio.getBaseCurrency(),
            holdings
        );
    }

    private HoldingPosition toPosition(HoldingEntity holding) {
        InstrumentEntity instrument = holding.getInstrument();
        return new HoldingPosition(
            holding.getId(),
            instrument.getId(),
            instrument.getAssetType(),
            instrument.getTicker(),
            instrument.getProviderQuoteId(),
            holding.getQuantity(),
            holding.getAveragePurchasePrice()
        );
    }
}
