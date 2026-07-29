package com.holdhive.analysis.domain.model;

import java.math.BigDecimal;

/**
 * A single holding's facts as consumed by the domain calculators.
 *
 * <p>This is intentionally decoupled from the API request DTO: the domain
 * layer must not depend on {@code api.dto} types (see backend/README convention
 * for domain/ being framework- and transport-agnostic). The application layer
 * is responsible for mapping {@code AnalyzePortfolioRequest.HoldingInput} to
 * this type.
 *
 * @param ticker      instrument or fund code, e.g. "600519"
 * @param assetType   coarse asset classification
 * @param quantity    number of units held (informational, not used in valuation here)
 * @param marketValue current market value in the portfolio's base currency, supplied by the caller
 * @param costBasis   total cost basis of this holding in the portfolio's base currency
 *                   (same currency as {@code marketValue}), may be {@code null} if not provided
 */
public record HoldingFact(
        String ticker,
        AssetType assetType,
        BigDecimal quantity,
        BigDecimal marketValue,
        BigDecimal costBasis
) {
}
