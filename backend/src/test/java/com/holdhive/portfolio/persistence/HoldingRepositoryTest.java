package com.holdhive.portfolio.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.portfolio.persistence.entity.HoldingEntity;
import com.holdhive.portfolio.persistence.entity.InstrumentEntity;
import com.holdhive.portfolio.persistence.entity.PortfolioEntity;
import com.holdhive.portfolio.persistence.repository.HoldingRepository;
import com.holdhive.portfolio.persistence.repository.InstrumentRepository;
import com.holdhive.portfolio.persistence.repository.PortfolioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HoldingRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Test
    void savesQueriesAndDeletesHoldingWithoutDeletingInstrument() {
        PortfolioEntity portfolio = portfolioRepository.findFirstByOrderByIdAsc().orElseThrow();
        InstrumentEntity instrument = instrumentRepository.saveAndFlush(new InstrumentEntity(
            AssetType.CRYPTO,
            "ETH_REPO_TEST",
            "CRYPTO",
            "Ethereum Test",
            "DEMO",
            "CRYPTO:ETH",
            "USD"
        ));
        HoldingEntity holding = holdingRepository.saveAndFlush(new HoldingEntity(
            portfolio,
            instrument,
            new BigDecimal("1.50000000"),
            new BigDecimal("3000.00000000")
        ));

        assertThat(holdingRepository.findByPortfolioId(portfolio.getId()))
            .extracting(HoldingEntity::getId)
            .contains(holding.getId());

        holdingRepository.delete(holding);
        holdingRepository.flush();

        assertThat(holdingRepository.findById(holding.getId())).isEmpty();
        assertThat(instrumentRepository.findById(instrument.getId())).isPresent();
    }

    @Test
    void reliesOnDatabaseConstraintForDuplicatePortfolioInstrumentHoldings() {
        PortfolioEntity portfolio = portfolioRepository.findFirstByOrderByIdAsc().orElseThrow();
        InstrumentEntity instrument = instrumentRepository.saveAndFlush(new InstrumentEntity(
            AssetType.STOCK,
            "DUP_REPO_TEST",
            "NASDAQ",
            "Duplicate Test",
            "EASTMONEY",
            "105.DUP_REPO_TEST",
            "USD"
        ));
        holdingRepository.saveAndFlush(new HoldingEntity(
            portfolio,
            instrument,
            BigDecimal.ONE,
            BigDecimal.TEN
        ));

        assertThatThrownBy(() -> holdingRepository.saveAndFlush(new HoldingEntity(
            portfolio,
            instrument,
            BigDecimal.ONE,
            BigDecimal.TEN
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
