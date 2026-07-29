package com.holdhive.portfolio.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.holdhive.portfolio.persistence.entity.PriceSnapshotEntity;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshotEntity, Long> {

    Optional<PriceSnapshotEntity> findFirstByInstrumentIdOrderByObservedAtDesc(Long instrumentId);

    @EntityGraph(attributePaths = "instrument")
    Optional<PriceSnapshotEntity> findFirstByInstrument_ProviderQuoteIdIgnoreCaseOrderByObservedAtDesc(
        String providerQuoteId
    );
}
