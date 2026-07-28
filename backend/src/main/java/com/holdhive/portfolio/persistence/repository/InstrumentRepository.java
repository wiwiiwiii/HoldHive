package com.holdhive.portfolio.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.portfolio.persistence.entity.InstrumentEntity;

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Long> {

    Optional<InstrumentEntity> findByAssetTypeAndTickerAndExchangeCode(
        AssetType assetType,
        String ticker,
        String exchangeCode
    );
}
