package com.holdhive.portfolio.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.holdhive.portfolio.persistence.entity.HoldingEntity;

public interface HoldingRepository extends JpaRepository<HoldingEntity, Long> {

    @EntityGraph(attributePaths = {"portfolio", "instrument"})
    List<HoldingEntity> findByPortfolioId(Long portfolioId);

    @EntityGraph(attributePaths = {"portfolio", "instrument"})
    Optional<HoldingEntity> findWithPortfolioAndInstrumentById(Long id);

    boolean existsByPortfolioIdAndInstrumentId(Long portfolioId, Long instrumentId);
}
