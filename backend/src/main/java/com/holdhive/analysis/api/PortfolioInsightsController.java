package com.holdhive.analysis.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.holdhive.analysis.application.CurrentPortfolioFactsProvider;
import com.holdhive.analysis.application.CurrentPortfolioFactsProvider.CurrentPortfolioHoldings;
import com.holdhive.analysis.application.PortfolioAnalysisFacts;
import com.holdhive.analysis.application.PortfolioAnalysisService;

/**
 * Serves the layered portfolio analysis (L0-L4 + sector exposure) for the
 * app's own current default portfolio - unlike {@link AnalysisController},
 * holdings are read server-side (via {@link CurrentPortfolioFactsProvider})
 * rather than supplied by the caller, so a client cannot inject an arbitrary
 * market value into the analysis.
 */
@RestController
@RequestMapping("/api/v1/portfolio/analysis")
public class PortfolioInsightsController {

    private final CurrentPortfolioFactsProvider currentPortfolioFactsProvider;
    private final PortfolioAnalysisService portfolioAnalysisService;

    public PortfolioInsightsController(
            CurrentPortfolioFactsProvider currentPortfolioFactsProvider,
            PortfolioAnalysisService portfolioAnalysisService) {
        this.currentPortfolioFactsProvider = currentPortfolioFactsProvider;
        this.portfolioAnalysisService = portfolioAnalysisService;
    }

    /**
     * Structured L0-L4 (+ sector exposure) facts for the current portfolio.
     * No LLM call is made here - this is deterministic and fast.
     */
    @GetMapping("/insights")
    public PortfolioAnalysisFacts insights() {
        CurrentPortfolioHoldings holdings = currentPortfolioFactsProvider.currentHoldings();
        return portfolioAnalysisService.computeFacts(holdings.holdings());
    }

    /**
     * Streams the L5 LLM narrative for the current portfolio as
     * {@code text/event-stream}. Each SSE {@code data:} event carries one
     * text chunk; the stream ends with completion (no LLM key configured or
     * any call failure degrades to a single explanatory chunk rather than an
     * HTTP error, matching the facts-only fallback used elsewhere in this
     * module).
     */
    @GetMapping(value = "/insights/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter insightsStream() {
        SseEmitter emitter = new SseEmitter(180_000L);
        CurrentPortfolioHoldings holdings = currentPortfolioFactsProvider.currentHoldings();

        Thread.ofVirtual().start(() -> portfolioAnalysisService.streamNarrative(
                holdings.baseCurrency(),
                holdings.holdings(),
                token -> sendSafely(emitter, token),
                emitter::complete,
                emitter::completeWithError));

        return emitter;
    }

    private void sendSafely(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (Exception e) {
            // Client likely disconnected mid-stream; nothing further to do here,
            // the emitter is already in a terminal state at that point.
        }
    }
}
