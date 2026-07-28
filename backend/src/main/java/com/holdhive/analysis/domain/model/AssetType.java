package com.holdhive.analysis.domain.model;

/**
 * Coarse asset classification used by the analysis module. Mirrors the
 * {@code asset_type} values used elsewhere in HoldHive (see
 * docs/qa/asset-types-scope.md) so this module stays vocabulary-compatible
 * with the main backend when it is eventually migrated in.
 */
public enum AssetType {
    STOCK,
    ETF,
    FUND,
    CASH,
    TERM_DEPOSIT,
    CRYPTO
}
