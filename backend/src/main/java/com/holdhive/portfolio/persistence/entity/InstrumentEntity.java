package com.holdhive.portfolio.persistence.entity;

import java.time.Instant;

import com.holdhive.portfolio.domain.AssetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "instrument")
public class InstrumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String ticker;

    @Column(name = "exchange_code", nullable = false, length = 16)
    private String exchangeCode = "UNKNOWN";

    @Column(name = "display_name", length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 24)
    private AssetType assetType = AssetType.STOCK;

    @Column(length = 32)
    private String provider;

    @Column(name = "provider_quote_id", length = 64)
    private String providerQuoteId;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstrumentEntity() {
    }

    public InstrumentEntity(
        AssetType assetType,
        String ticker,
        String exchangeCode,
        String displayName,
        String provider,
        String providerQuoteId,
        String currency
    ) {
        this.assetType = assetType;
        this.ticker = ticker;
        this.exchangeCode = exchangeCode;
        this.displayName = displayName;
        this.provider = provider;
        this.providerQuoteId = providerQuoteId;
        this.currency = currency;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public String getExchangeCode() {
        return exchangeCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderQuoteId() {
        return providerQuoteId;
    }

    public String getCurrency() {
        return currency;
    }

    public void refreshMarketMetadata(
        String displayName,
        String provider,
        String providerQuoteId,
        String currency
    ) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (provider != null) {
            this.provider = provider;
        }
        if (providerQuoteId != null) {
            this.providerQuoteId = providerQuoteId;
        }
        if (currency != null) {
            this.currency = currency;
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
