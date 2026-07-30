package com.holdhive.analysis.api;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holdhive.analysis.application.CurrentPortfolioFactsProvider;
import com.holdhive.analysis.application.CurrentPortfolioFactsProvider.CurrentPortfolioHoldings;
import com.holdhive.analysis.application.PortfolioAnalysisFacts;
import com.holdhive.analysis.application.PortfolioAnalysisService;
import com.holdhive.pricing.application.PriceMode;

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
    private final ObjectMapper objectMapper;

    public PortfolioInsightsController(
            CurrentPortfolioFactsProvider currentPortfolioFactsProvider,
            PortfolioAnalysisService portfolioAnalysisService,
            ObjectMapper objectMapper) {
        this.currentPortfolioFactsProvider = currentPortfolioFactsProvider;
        this.portfolioAnalysisService = portfolioAnalysisService;
        this.objectMapper = objectMapper;
    }

    /**
     * Structured L0-L4 (+ sector exposure) facts for the current portfolio.
     * No LLM call is made here - this is deterministic and fast.
     */
    @GetMapping("/insights")
    public PortfolioAnalysisFacts insights(
            @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode
    ) {
        CurrentPortfolioHoldings holdings = currentPortfolioFactsProvider.currentHoldings(priceMode);
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
    public SseEmitter insightsStream(
            @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode
    ) {
        SseEmitter emitter = new SseEmitter(180_000L);
        CurrentPortfolioHoldings holdings = currentPortfolioFactsProvider.currentHoldings(priceMode);

        Thread.ofVirtual().start(() -> portfolioAnalysisService.streamNarrative(
                holdings.baseCurrency(),
                holdings.holdings(),
                token -> sendSafely(emitter, token),
                emitter::complete,
                emitter::completeWithError));

        return emitter;
    }

    /**
     * Combined SSE endpoint: sends the structured L0-L4 facts as the first
     * event, then streams the L5 LLM narrative as subsequent events. This
     * avoids the double database query and double fund-holdings prefetch
     * that would occur if the frontend called {@code /insights} and
     * {@code /insights/stream} concurrently.
     *
     * <p>Event sequence:
     * <ol>
     *   <li>{@code event: facts} - JSON payload with all L0-L4 structured data</li>
     *   <li>{@code event: token} - one text chunk per LLM stream token</li>
     *   <li>{@code event: done} - stream completed successfully</li>
     * </ol>
     * If the LLM call fails, the client still receives the facts event and
     * can render the data cards; only the narrative section is degraded.
     */
    @GetMapping(value = "/insights/full", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter insightsFull(
            @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode
    ) {
        SseEmitter emitter = new SseEmitter(180_000L);

        // 1. Fetch holdings once (single DB query)
        CurrentPortfolioHoldings holdings = currentPortfolioFactsProvider.currentHoldings(priceMode);

        // 2. Compute facts once (single fund-holdings prefetch)
        PortfolioAnalysisFacts facts = portfolioAnalysisService.computeFacts(holdings.holdings());

        // 3. Send facts as the first SSE event (reaches frontend in ~ms)
        try {
            String factsJson = objectMapper.writeValueAsString(
                    Map.of("type", "facts", "payload", facts));
            emitter.send(SseEmitter.event()
                    .name("facts")
                    .data(factsJson));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // 4. Stream LLM narrative using the pre-computed facts (no re-computation)
        Thread.ofVirtual().start(() -> portfolioAnalysisService.streamNarrative(
                holdings.baseCurrency(),
                holdings.holdings(),
                facts,
                token -> sendTokenSafely(emitter, token),
                () -> sendDoneSafely(emitter),
                emitter::completeWithError));

        return emitter;
    }

    private void sendTokenSafely(SseEmitter emitter, String token) {
        try {
            String tokenJson = objectMapper.writeValueAsString(
                    Map.of("type", "token", "payload", token));
            emitter.send(SseEmitter.event()
                    .name("token")
                    .data(tokenJson));
        } catch (Exception e) {
            // Client likely disconnected mid-stream
        }
    }

    private void sendDoneSafely(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data("{\"type\":\"done\"}"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
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
