package com.holdhive.analysis.application;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdhive.analysis.api.dto.AnalyzePortfolioRequest;
import com.holdhive.analysis.api.dto.AnalyzePortfolioRequest.HoldingInput;
import com.holdhive.analysis.api.dto.PortfolioAnalysisResponse;
import com.holdhive.analysis.domain.ConcentrationCalculator;
import com.holdhive.analysis.domain.ConcentrationCalculator.ConcentrationResult;
import com.holdhive.analysis.domain.FundHoldingsLookup;
import com.holdhive.analysis.domain.FundOverlapCalculator;
import com.holdhive.analysis.domain.FundOverlapCalculator.FundOverlapResult;
import com.holdhive.analysis.domain.LookThroughCalculator;
import com.holdhive.analysis.domain.LookThroughCalculator.LookThroughResult;
import com.holdhive.analysis.domain.OverviewCalculator;
import com.holdhive.analysis.domain.OverviewCalculator.OverviewResult;
import com.holdhive.analysis.domain.ProfitLossCalculator;
import com.holdhive.analysis.domain.ProfitLossCalculator.ProfitLossResult;
import com.holdhive.analysis.domain.SectorExposureCalculator;
import com.holdhive.analysis.domain.SectorExposureCalculator.SectorExposureResult;
import com.holdhive.analysis.domain.SectorLookup;
import com.holdhive.analysis.domain.model.AssetType;
import com.holdhive.analysis.domain.model.FundHoldingSnapshot;
import com.holdhive.analysis.domain.model.HoldingFact;
import com.holdhive.analysis.infrastructure.llm.DeepSeekClient;

/**
 * Orchestrates the layered portfolio analysis:
 * <ol>
 *   <li>L0 {@link OverviewCalculator} - total value + allocation by asset type</li>
 *   <li>L1 {@link ConcentrationCalculator} - HHI + top holding + top-N combined facts</li>
 *   <li>L2 {@link FundOverlapCalculator} - fund vs. direct-stock overlap</li>
 *   <li>L3 {@link LookThroughCalculator} - effective exposures through fund holdings</li>
 *   <li>L3b {@link SectorExposureCalculator} - sector/industry exposure (direct + look-through)</li>
 *   <li>L4 {@link ProfitLossCalculator} - unrealized P&amp;L per holding</li>
 *   <li>L5 {@link DeepSeekClient} - narrative-only interpretation of the above facts</li>
 * </ol>
 *
 * <p><b>Key design rule:</b> structured L0-L4 facts are computed exclusively in
 * Java and must never be recomputed by the LLM. In narrative text (L5) the LLM
 * is allowed simple derivations (sums of two percentages, rough comparisons)
 * as long as they are marked with "approximately""roughly" etc. Multi-step arithmetic
 * (HHI, weighted averages, sums of 4+ numbers) remains off-limits.
 * If the LLM call fails for any reason, the L0-L4 facts
 * are still returned (HTTP 200) with {@code warning=LLM_UNAVAILABLE}.
 */
@Service
public class PortfolioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAnalysisService.class);

    private static final String SYSTEM_PROMPT = """
            You are HoldHive's portfolio analysis assistant. The facts fields in the user \
            message (total market value, asset class breakdown, HHI concentration index, \
            largest holding percentage, top-N holdings and their combined percentage, \
            fund-stock overlap value and percentage, effective individual stock holding \
            percentages after unpacking funds along with effective HHI, sector-level \
            effective holding percentages and sector HHI including the look-through-adjusted \
            view, and unrealized P&L for each holding) have all been precisely computed \
            by the program.\
            [Rule 1 · Citation] Every individual number already present in facts (HHI, \
            holding percentages, P&L amounts/returns, sector percentages, total market \
            value) must be quoted exactly as-is — no rounding or modification.\
            [Rule 1 · Derivation] You may perform simple numeric derivations to make the \
            narrative sound more like a human analyst: adding or subtracting two percentages \
            (e.g., "Moutai and CATL combined account for approximately 45%"), simple ratio \
            comparisons (e.g., "Moutai's market value is roughly 2× that of Midea"). Derived \
            numbers must be marked with qualifiers such as "approximately", "roughly", or \
            "nearly" to distinguish them from precisely-quoted numbers. Multi-step arithmetic \
            is forbidden: no weighted averages, no summing 4+ numbers, no recomputing HHI \
            coefficients or post-look-through holding percentages (these are guaranteed by \
            the program). Derivations must be based solely on numbers appearing in facts \
            — never fabricate or extrapolate data outside facts. When you need "top-N \
            combined percentage", prefer directly quoting concentration.topHoldingsCombinedPercent \
            — it is more reliable than manual summation.\
            [Rule 2] When describing a stock's actual holding percentage, you must use the \
            lookThrough effective measure (which already incorporates indirect holdings from \
            funds), not the entry-level percentage.\
            [Rule 3] Qualitative concentration wording must strictly match \
            concentration.riskLevel: LOW must be described as "low concentration", MEDIUM \
            as "moderate concentration", HIGH as "high concentration". Never use terms like \
            "somewhat high", "somewhat low", "relatively high", or "elevated" that could \
            contradict riskLevel. When citing an HHI value, include the threshold reference \
            (below 0.15 is low, 0.15–0.25 is moderate, above 0.25 is high). When referring \
            to "top-N", cross-check concentration.holdingCount against topHoldings: if both \
            counts are equal (i.e., topHoldings already covers all holdings), say "all \
            holdings (N items in total)" rather than "top N".\
            Your task is to generate concise, professional English commentary based on these \
            facts, with a natural tone.\
            Output only a single JSON object (no markdown code block markers), with the \
            following fields:\
            {"summary": "One-sentence overall summary, max 150 characters", \
            "riskCommentary": "Commentary on concentration risk (HHI/largest holding/top-N \
            combined %), qualitative wording strictly follows Rule 3, max 300 characters", \
            "sectorCommentary": "Commentary on sector-level effective holding percentages \
            — must name sectorExposure.topSector and its topSectorPercent, and describe \
            sector concentration or diversification based on sector HHI; if sector coverage \
            is low (sectorExposure.attributedPercent is low), note that a large portion of \
            holdings has unknown sector classification, max 350 characters", \
            "fundOverlapCommentary": "If the portfolio contains no FUND-type holdings, \
            output an empty string \\"\\" — do not write boilerplate like \\"not applicable\\"; \
            do not repeat numbers from other sections here; otherwise, provide commentary on \
            fund-stock overlap — if unavailableFunds is non-empty, note the data gap; if no \
            overlap exists, state that no overlap was detected, max 300 characters", \
            "lookThroughCommentary": "If the portfolio contains no FUND-type holdings, \
            output an empty string \\"\\" — no boilerplate; do not repeat numbers from other \
            sections here; otherwise, provide commentary on effective holding percentages \
            after unpacking funds — must reference effectiveHhi/topHolding/\
            attributedPercentOfPortfolio etc.; if attributedPercent is low, warn that the \
            proportion of holdings with traceable underlying constituents is insufficient, \
            max 350 characters", \
            "profitLossCommentary": "Commentary on P&L; if missingCostBasisTickers is \
            non-empty, note the data gap, max 250 characters", \
            "diversificationAdvice": "1–3 specific, actionable diversification suggestions, \
            separated by semicolons; you may use derived numbers to give quantitative targets \
            (e.g., \\"reduce a holding's weight to below 20%\\"), with derived numbers \
            following Rule 1's marking rules", \
            "actionSuggestions": ["Suggestion 1", "Suggestion 2"]}
            """;

    /**
     * Streaming variant of {@link #SYSTEM_PROMPT}: asks for Markdown-formatted
     * English prose instead of a JSON object, so tokens can be forwarded to the
     * client as they arrive (a partial JSON document is not safely renderable,
     * partial Markdown is). Keeps the same citation/derivation/wording rules as
     * the blocking prompt so the two remain factually consistent with each other.
     */
    private static final String STREAM_SYSTEM_PROMPT = """
            You are HoldHive's portfolio analysis assistant. The facts fields in the user \
            message (total market value, asset class breakdown, HHI concentration index, \
            largest holding percentage, top-N holdings and their combined percentage, \
            fund-stock overlap value and percentage, effective individual stock holding \
            percentages after unpacking funds along with effective HHI, sector-level \
            effective holding percentages and sector HHI including the look-through-adjusted \
            view, and unrealized P&L for each holding) have all been precisely computed \
            by the program.\
            [Rule 1 · Citation] Every individual number already present in facts (HHI, \
            holding percentages, P&L amounts/returns, sector percentages, total market \
            value) must be quoted exactly as-is — no rounding or modification.\
            [Rule 1 · Derivation] You may perform simple numeric derivations to make the \
            narrative sound more like a human analyst: adding or subtracting two percentages, \
            simple ratio comparisons. Derived numbers must be marked with qualifiers such \
            as "approximately", "roughly", or "nearly". Multi-step arithmetic is forbidden: \
            no weighted averages, no summing 4+ numbers, no recomputing HHI coefficients \
            or post-look-through holding percentages. When you need "top-N combined \
            percentage", prefer directly quoting concentration.topHoldingsCombinedPercent.\
            [Rule 2] When describing a stock's actual holding percentage, you must use the \
            lookThrough effective measure.\
            [Rule 3] Qualitative concentration wording must strictly match \
            concentration.riskLevel: LOW must be described as "low concentration", MEDIUM \
            as "moderate concentration", HIGH as "high concentration". Never use terms like \
            "somewhat high", "somewhat low", "relatively high", or "elevated" that could \
            contradict riskLevel. When citing an HHI value, include the threshold reference \
            (below 0.15 is low, 0.15–0.25 is moderate, above 0.25 is high).\
            Your task is to generate a professional English commentary based on these \
            facts, with a natural tone, like a human analyst's report.\
            Output in Markdown format (no JSON, no code block markers). Use `###` headings \
            for each section, **bold** for key figures (HHI values, percentages, tickers), \
            and bullet lists for diversification suggestions. Structure as follows:\
            ### Portfolio Overview — one paragraph: total market value, asset class \
            breakdown, overall P&L summary; note missing cost basis if applicable;\
            ### Concentration Risk — HHI, largest holding, top-N combined %, risk level;\
            ### Sector Exposure — sectorExposure.topSector and its percentage, sector \
            concentration/diversification; note low coverage if applicable;\
            ### Fund Analysis — if no FUND-type holdings, omit this section entirely \
            (no "not applicable" boilerplate); otherwise cover fund-stock overlap and \
            effective holding percentages after unpacking funds in one cohesive section;\
            ### Diversification Advice — 1–3 specific, actionable suggestions as bullet \
            points.\
            Keep the entire output under 1,200 characters.
            """;

    private final OverviewCalculator overviewCalculator = new OverviewCalculator();
    private final ConcentrationCalculator concentrationCalculator = new ConcentrationCalculator();
    private final FundOverlapCalculator fundOverlapCalculator = new FundOverlapCalculator();
    private final LookThroughCalculator lookThroughCalculator = new LookThroughCalculator();
    private final SectorExposureCalculator sectorExposureCalculator = new SectorExposureCalculator();
    private final ProfitLossCalculator profitLossCalculator = new ProfitLossCalculator();

    private final FundHoldingsLookup fundHoldingsLookup;
    private final SectorLookup sectorLookup;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    public PortfolioAnalysisService(
            FundHoldingsLookup fundHoldingsLookup,
            SectorLookup sectorLookup,
            DeepSeekClient deepSeekClient,
            ObjectMapper objectMapper) {
        this.fundHoldingsLookup = fundHoldingsLookup;
        this.sectorLookup = sectorLookup;
        this.deepSeekClient = deepSeekClient;
        this.objectMapper = objectMapper;
    }

    public PortfolioAnalysisResponse analyze(AnalyzePortfolioRequest request) {
        List<HoldingFact> facts = request.holdings().stream()
                .map(this::toFact)
                .toList();

        PortfolioAnalysisFacts result = computeFacts(facts);

        String userPrompt = buildUserPrompt(request.baseCurrencyOrDefault(), facts, result);
        Optional<JsonNode> narrative = deepSeekClient.requestNarrative(SYSTEM_PROMPT, userPrompt);

        if (narrative.isEmpty()) {
            log.info("Returning facts-only portfolio analysis (LLM unavailable)");
            return new PortfolioAnalysisResponse(result.overview(), result.concentration(), result.fundOverlap(), result.lookThrough(), result.sectorExposure(), result.profitLoss(), null, "LLM_UNAVAILABLE");
        }
        return new PortfolioAnalysisResponse(result.overview(), result.concentration(), result.fundOverlap(), result.lookThrough(), result.sectorExposure(), result.profitLoss(), narrative.get(), null);
    }

    /**
     * Computes the deterministic L0-L4 (+L3b sector) facts for an arbitrary
     * holding list - no LLM call. Shared by the blocking {@link #analyze}
     * entrypoint and by callers that only need structured facts (the
     * {@code /insights} endpoint) or need the facts merely to build an LLM
     * prompt (the streaming {@code /insights/stream} endpoint).
     */
    public PortfolioAnalysisFacts computeFacts(List<HoldingFact> facts) {
        // Fetch every distinct FUND holding's snapshot exactly once, in parallel,
        // instead of letting each of the three fund-aware calculators below call
        // fundHoldingsLookup.find() independently (previously relying on
        // fundHoldingsLookup's own cache being incidentally warmed by whichever
        // calculator happened to run first).
        FundHoldingsLookup prefetchedFundLookup = prefetchFundHoldings(facts);

        OverviewResult overview = overviewCalculator.calculate(facts);
        ConcentrationResult concentration = concentrationCalculator.calculate(facts);
        FundOverlapResult fundOverlap = fundOverlapCalculator.calculate(facts, prefetchedFundLookup);
        LookThroughResult lookThrough = lookThroughCalculator.calculate(facts, prefetchedFundLookup);
        SectorExposureResult sectorExposure = sectorExposureCalculator.calculate(facts, sectorLookup, prefetchedFundLookup);
        ProfitLossResult profitLoss = profitLossCalculator.calculate(facts);

        return new PortfolioAnalysisFacts(overview, concentration, fundOverlap, lookThrough, sectorExposure, profitLoss);
    }

    /**
     * Streams a free-form Chinese narrative for the given holdings via DeepSeek
     * (see {@link DeepSeekClient#streamNarrative}). {@code onToken} is invoked
     * once per received text chunk (in order); exactly one of {@code onComplete}
     * or {@code onError} is invoked exactly once at the end.
     */
    public void streamNarrative(
            String baseCurrency,
            List<HoldingFact> holdings,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError) {
        PortfolioAnalysisFacts facts = computeFacts(holdings);
        streamNarrative(baseCurrency, holdings, facts, onToken, onComplete, onError);
    }

    /**
     * Streaming variant that accepts externally computed facts, avoiding a
     * redundant {@link #computeFacts} call when the caller has already computed
     * them (e.g. the combined {@code /insights/full} endpoint).
     */
    public void streamNarrative(
            String baseCurrency,
            List<HoldingFact> holdings,
            PortfolioAnalysisFacts precomputedFacts,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError) {
        String userPrompt = buildUserPrompt(baseCurrency, holdings, precomputedFacts);
        deepSeekClient.streamNarrative(STREAM_SYSTEM_PROMPT, userPrompt, onToken, onComplete, onError);
    }

    private HoldingFact toFact(HoldingInput input) {
        return new HoldingFact(input.ticker(), input.assetType(), input.quantity(), input.marketValue(), input.costBasis());
    }

    /**
     * Fetches every distinct FUND holding's snapshot exactly once per request,
     * fanning the calls out across virtual threads so that a live lookup
     * implementation (e.g. the EastMoney provider, which is network-backed)
     * pays its latency once and in parallel rather than once per calculator.
     * Returns a lookup backed purely by the resulting in-memory map so
     * {@link FundOverlapCalculator}, {@link LookThroughCalculator} and
     * {@link SectorExposureCalculator} see identical, already-resolved data
     * regardless of call order.
     */
    private FundHoldingsLookup prefetchFundHoldings(List<HoldingFact> facts) {
        List<String> fundTickers = facts.stream()
                .filter(f -> f.assetType() == AssetType.FUND)
                .map(HoldingFact::ticker)
                .distinct()
                .toList();

        if (fundTickers.isEmpty()) {
            return ticker -> Optional.empty();
        }

        Map<String, Optional<FundHoldingSnapshot>> snapshots = new HashMap<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<String, Future<Optional<FundHoldingSnapshot>>> pending = new LinkedHashMap<>();
            for (String ticker : fundTickers) {
                pending.put(ticker, executor.submit(() -> fundHoldingsLookup.find(ticker)));
            }
            for (Map.Entry<String, Future<Optional<FundHoldingSnapshot>>> entry : pending.entrySet()) {
                snapshots.put(entry.getKey(), awaitFundLookup(entry.getKey(), entry.getValue()));
            }
        }
        return ticker -> snapshots.getOrDefault(ticker, Optional.empty());
    }

    private Optional<FundHoldingSnapshot> awaitFundLookup(String ticker, Future<Optional<FundHoldingSnapshot>> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Fund holdings prefetch interrupted for {}", ticker);
            return Optional.empty();
        } catch (ExecutionException e) {
            log.warn("Fund holdings prefetch failed for {}: {}", ticker, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return Optional.empty();
        }
    }

    private String buildUserPrompt(String baseCurrency, List<HoldingFact> holdings, PortfolioAnalysisFacts facts) {
        try {
            Map<String, Object> payload = Map.of(
                    "baseCurrency", baseCurrency,
                    "holdings", holdings,
                    "facts", Map.of(
                            "overview", facts.overview(),
                            "concentration", facts.concentration(),
                            "fundOverlap", facts.fundOverlap(),
                            "lookThrough", facts.lookThrough(),
                            "sectorExposure", facts.sectorExposure(),
                            "profitLoss", facts.profitLoss()));
            return "Generate commentary based on the following facts:\n" + objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize facts payload for LLM prompt", e);
        }
    }
}
