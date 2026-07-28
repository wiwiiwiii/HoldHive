package com.holdhive.portfolio.domain;

public enum AssetType {
    STOCK,
    ETF,
    MUTUAL_FUND,
    CRYPTO,
    CASH,
    BANK_DEPOSIT;

    public boolean isFixedValueAsset() {
        return this == CASH || this == BANK_DEPOSIT;
    }

    public boolean isFundLike() {
        return this == ETF || this == MUTUAL_FUND;
    }
}
