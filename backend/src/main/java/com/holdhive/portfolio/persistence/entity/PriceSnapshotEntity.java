package com.holdhive.portfolio.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_snapshot")
public class PriceSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private InstrumentEntity instrument;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PriceSnapshotEntity() {
    }

    public PriceSnapshotEntity(
        InstrumentEntity instrument,
        BigDecimal price,
        String currency,
        String provider,
        boolean demo,
        Instant observedAt
    ) {
        this.instrument = instrument;
        this.price = price;
        this.currency = currency;
        this.provider = provider;
        this.demo = demo;
        this.observedAt = observedAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public InstrumentEntity getInstrument() {
        return instrument;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProvider() {
        return provider;
    }

    public boolean isDemo() {
        return demo;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
