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
 * as long as they are marked with "约""合计约" etc. Multi-step arithmetic
 * (HHI, weighted averages, sums of 4+ numbers) remains off-limits.
 * If the LLM call fails for any reason, the L0-L4 facts
 * are still returned (HTTP 200) with {@code warning=LLM_UNAVAILABLE}.
 */
@Service
public class PortfolioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAnalysisService.class);

    private static final String SYSTEM_PROMPT = """
            你是 HoldHive 的投资组合分析助手。用户消息中的 facts 字段（总市值、各资产类别占比、\
            HHI 集中度指数、最大持仓占比、前N大持仓及其合计占比、基金与个股的重叠市值及占比、\
            拆开基金后的实际个股持有比例及实际集中度指数（有效HHI）、\
            行业维度的实际持有比例及行业集中度指数（含拆开基金后的口径）、\
            各持仓的浮动盈亏）均已由程序精确计算完成。\
            【规则一 · 引用规则】facts 中已有的单个数字（HHI、各项持仓占比、盈亏金额/收益率、行业占比、\
            市值合计）必须原样精确引用，不得四舍五入或修改。\
            【规则一 · 派生规则】允许你做简单的数值派生，让表述更贴近真人分析师的语言习惯：\
            两个占比相加、相减（如"茅台和宁德的合计约45%"）、\
            简单的倍数/比例比较（如"茅台市值是美的的2倍")；\
            派生出的数字必须用"约""合计约""近"等词标记，与精确引用的数字区分开。\
            禁止多步运算：不得计算加权平均、不得把3个以上的数字连加、\
            不得自行重算 HHI 系数或拆基金后的持有比例（这些由程序保证）。\
            派生只能基于 facts 中出现的数字，不得编造或推测 facts 之外的数据。\
            需要"前N大合计占比"时优先直接引用 concentration.topHoldingsCombinedPercent，\
            它比逐项累加更可靠，应优先直接引用。\
            【规则二】表述某只股票的实际持有比例时，必须以 lookThrough 的 effective 口径为准\
            （该数字已经把基金里间接持有的部分折算进去了），而非条目级占比。\
            【规则三】描述集中度的定性措辞必须与 concentration.riskLevel 严格一致：\
            LOW 必须说"集中度低"，MEDIUM 必须说"集中度中等"，HIGH 必须说"集中度高"；\
            在任何栏目中都不得使用"偏高""偏低""较高""过高"等可能与 riskLevel 相矛盾的措辞；\
            引用 HHI 数值时需附带阈值参照（小于0.15为低，0.15至0.25为中，大于0.25为高）。\
            表述"前N大"时需对照 concentration.holdingCount 与 topHoldings：\
            若两者数量相同（即 topHoldings 已覆盖全部持仓），应表述为"全部持仓（共N项）"而不是"前N大"。\
            你的任务是基于这些事实生成简洁、专业的中文解读，语气自然。\
            请仅输出一个 JSON 对象（不要包含 markdown 代码块标记），字段如下：\
            {"summary": "整体一句话总结，不超过80字", \
            "riskCommentary": "针对集中度风险(HHI/最大持仓/前N大合计占比)的解读，定性措辞严格遵循规则三，不超过120字", \
            "sectorCommentary": "针对行业实际持有比例的解读，必须点名 sectorExposure.topSector 及其 topSectorPercent，并基于行业HHI说明行业集中或分散情况；若行业覆盖度偏低(sectorExposure中 attributedPercent 较低)需说明有大量持仓的行业归属未知，不超过140字", \
            "fundOverlapCommentary": "若组合中没有任何 FUND 类型持仓，直接输出空字符串\"\"，不要写\"不适用\"之类的套话；不得在此栏复述其他栏目的数字；否则针对基金与个股重叠进行解读，若 unavailableFunds 非空需说明数据缺口，若无重叠需说明当前未发现重叠，不超过120字", \
            "lookThroughCommentary": "若组合中没有任何 FUND 类型持仓，直接输出空字符串\"\"，不要写套话；不得在此栏复述其他栏目的数字；否则针对拆开基金后的实际持有比例进行解读，必须引用 effectiveHhi/topHolding/attributedPercentOfPortfolio 等事实，若 attributedPercent 偏低须提醒能查清楚底层持仓的比例不足，不超过140字", \
            "profitLossCommentary": "针对盈亏的解读；若 missingCostBasisTickers 非空须说明数据缺口，不超过100字", \
            "diversificationAdvice": "1-3条具体、可执行的分散化建议，用分号分隔；允许用派生数字给出量化目标（如\"将某持仓占比降至20%以下\"），派生数字遵循规则一的标记规则", \
            "actionSuggestions": ["建议1", "建议2"]}
            """;

    /**
     * Streaming variant of {@link #SYSTEM_PROMPT}: asks for continuous Chinese
     * prose instead of a JSON object, so tokens can be forwarded to the client
     * as they arrive (a partial JSON document is not safely renderable, partial
     * prose is). Keeps the same citation/derivation/wording rules as the
     * blocking prompt so the two remain factually consistent with each other.
     */
    private static final String STREAM_SYSTEM_PROMPT = """
            你是 HoldHive 的投资组合分析助手。用户消息中的 facts 字段（总市值、各资产类别占比、\
            HHI 集中度指数、最大持仓占比、前N大持仓及其合计占比、基金与个股的重叠市值及占比、\
            拆开基金后的实际个股持有比例及实际集中度指数（有效HHI）、\
            行业维度的实际持有比例及行业集中度指数（含拆开基金后的口径）、\
            各持仓的浮动盈亏）均已由程序精确计算完成。\
            【规则一 · 引用规则】facts 中已有的单个数字（HHI、各项持仓占比、盈亏金额/收益率、行业占比、\
            市值合计）必须原样精确引用，不得四舍五入或修改。\
            【规则一 · 派生规则】允许你做简单的数值派生，让表述更贴近真人分析师的语言习惯：\
            两个占比相加、相减、简单的倍数/比例比较；派生出的数字必须用"约""合计约""近"等词标记。\
            禁止多步运算：不得计算加权平均、不得把3个以上的数字连加、不得自行重算 HHI 系数或拆基金后的持有比例。\
            需要"前N大合计占比"时优先直接引用 concentration.topHoldingsCombinedPercent。\
            【规则二】表述某只股票的实际持有比例时，必须以 lookThrough 的 effective 口径为准。\
            【规则三】描述集中度的定性措辞必须与 concentration.riskLevel 严格一致：\
            LOW 必须说"集中度低"，MEDIUM 必须说"集中度中等"，HIGH 必须说"集中度高"；\
            不得使用"偏高""偏低""较高""过高"等可能与 riskLevel 相矛盾的措辞；引用 HHI 数值时需附带阈值参照\
            （小于0.15为低，0.15至0.25为中，大于0.25为高）。\
            你的任务是基于这些事实生成一段连续的中文解读，语气自然、专业，像真人分析师的报告。\
            请直接输出纯文本（不要输出 JSON、不要输出 markdown 代码块标记），按以下顺序自然衔接成段落：\
            1）整体总结；2）集中度风险解读（HHI/最大持仓/前N大合计占比）；\
            3）行业实际持有比例解读（须点名 sectorExposure.topSector 及其占比，覆盖度偏低需说明）；\
            4）基金与个股重叠解读（若组合中没有任何 FUND 类型持仓，跳过此段，不要写"不适用"之类的套话）；\
            5）拆开基金后的实际持有比例解读（若无 FUND 持仓同样跳过）；\
            6）盈亏解读（缺失成本数据需说明）；7）1-3条具体可执行的分散化建议。\
            全文控制在500字以内。
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
            return "请基于以下 facts 生成解读：\n" + objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize facts payload for LLM prompt", e);
        }
    }
}
