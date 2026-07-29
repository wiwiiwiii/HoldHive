package com.holdhive.analysis.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.holdhive.analysis.domain.ConcentrationCalculator.ConcentrationResult;
import com.holdhive.analysis.domain.FundOverlapCalculator.FundOverlapResult;
import com.holdhive.analysis.domain.LookThroughCalculator.LookThroughResult;
import com.holdhive.analysis.domain.OverviewCalculator.OverviewResult;
import com.holdhive.analysis.domain.ProfitLossCalculator.ProfitLossResult;
import com.holdhive.analysis.domain.SectorExposureCalculator.SectorExposureResult;

/**
 * Wire-format response body. {@code overview}/{@code concentration}/
 * {@code fundOverlap}/{@code lookThrough}/{@code profitLoss} are the
 * deterministic L0-L4 facts computed in Java; {@code llmInsights} is the
 * DeepSeek-generated narrative JSON layered on top (verbatim - see
 * {@code PortfolioAnalysisService} for the prompt that forbids the model
 * from recomputing any numbers). When the LLM call could not be completed,
 * {@code llmInsights} is {@code null} and {@code warning} explains why;
 * the L0-L4 facts are still returned with HTTP 200.
 *
 * TODO: this L0-L4 breakdown is quite granular (6 separate result objects);
 * consider simplifying/flattening the response shape for frontend consumption
 * once real usage patterns are clearer (e.g. merge overlapping fields between
 * {@code concentration}/{@code lookThrough}, or drop rarely-used sub-fields).
 */
public record PortfolioAnalysisResponse(
        OverviewResult overview,
        ConcentrationResult concentration,
        FundOverlapResult fundOverlap,
        LookThroughResult lookThrough,
        SectorExposureResult sectorExposure,
        ProfitLossResult profitLoss,
        JsonNode llmInsights,
        String warning
) {
}
