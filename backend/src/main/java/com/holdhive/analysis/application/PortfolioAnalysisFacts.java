package com.holdhive.analysis.application;

import com.holdhive.analysis.domain.ConcentrationCalculator.ConcentrationResult;
import com.holdhive.analysis.domain.FundOverlapCalculator.FundOverlapResult;
import com.holdhive.analysis.domain.LookThroughCalculator.LookThroughResult;
import com.holdhive.analysis.domain.OverviewCalculator.OverviewResult;
import com.holdhive.analysis.domain.ProfitLossCalculator.ProfitLossResult;
import com.holdhive.analysis.domain.SectorExposureCalculator.SectorExposureResult;

/**
 * Bundles the deterministic L0-L4 (+L3b sector) facts computed by
 * {@link PortfolioAnalysisService#computeFacts(java.util.List)}, shared by:
 * <ul>
 *   <li>the legacy request-driven {@code POST /api/v1/portfolio/analysis} endpoint (blocking, includes LLM narrative)</li>
 *   <li>the current-portfolio {@code GET /api/v1/portfolio/analysis/insights} endpoint (facts-only, no LLM call)</li>
 *   <li>the streaming {@code GET /api/v1/portfolio/analysis/insights/stream} endpoint (facts feed the LLM prompt only)</li>
 * </ul>
 */
public record PortfolioAnalysisFacts(
        OverviewResult overview,
        ConcentrationResult concentration,
        FundOverlapResult fundOverlap,
        LookThroughResult lookThrough,
        SectorExposureResult sectorExposure,
        ProfitLossResult profitLoss
) {
}
