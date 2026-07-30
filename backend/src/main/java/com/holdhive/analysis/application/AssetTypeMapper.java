package com.holdhive.analysis.application;

/**
 * Maps the main portfolio module's coarse asset classification
 * ({@link com.holdhive.portfolio.domain.AssetType}) to the analysis module's
 * own classification ({@link com.holdhive.analysis.domain.model.AssetType}).
 *
 * <p>The two enums are not identical - the analysis module models
 * "fund" and "term deposit" more coarsely (see {@code FUND}/{@code TERM_DEPOSIT})
 * than the portfolio module does ({@code MUTUAL_FUND}/{@code BANK_DEPOSIT}) -
 * so a request/response DTO on either side must never be reused directly across
 * the module boundary. This mapper is the single place that bridges them.
 */
public final class AssetTypeMapper {

    private AssetTypeMapper() {
    }

    public static com.holdhive.analysis.domain.model.AssetType toAnalysisAssetType(
            com.holdhive.portfolio.domain.AssetType portfolioAssetType) {
        return switch (portfolioAssetType) {
            case STOCK -> com.holdhive.analysis.domain.model.AssetType.STOCK;
            case ETF -> com.holdhive.analysis.domain.model.AssetType.ETF;
            case MUTUAL_FUND -> com.holdhive.analysis.domain.model.AssetType.FUND;
            case CRYPTO -> com.holdhive.analysis.domain.model.AssetType.CRYPTO;
            case CASH -> com.holdhive.analysis.domain.model.AssetType.CASH;
            case BANK_DEPOSIT -> com.holdhive.analysis.domain.model.AssetType.TERM_DEPOSIT;
        };
    }
}
