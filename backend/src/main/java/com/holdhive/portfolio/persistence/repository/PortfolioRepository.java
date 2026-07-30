package com.holdhive.portfolio.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.holdhive.portfolio.persistence.entity.PortfolioEntity;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, Long> {

    Optional<PortfolioEntity> findFirstByOrderByIdAsc();
}
