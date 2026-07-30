package com.holdhive.analysis.infrastructure.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin client for a DeepSeek (OpenAI-compatible) chat completions endpoint.
 *
 * <p>Deliberately narrow contract: callers supply a system prompt (which must
 * instruct the model to only narrate pre-computed facts, never to recompute
 * percentages/HHI/overlap itself - see {@code PortfolioAnalysisService}) and a
 * user prompt containing the L0-L2 facts as JSON. The raw parsed JSON body of
 * the model's reply is returned so the caller can merge it into the final
 * response; any failure (missing key, timeout, non-200, unparsable content)
 * results in {@link Optional#empty()} so the caller can fall back to
 * facts-only output instead of failing the whole request.
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final RestTemplate restTemplate;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(RestTemplate deepSeekRestTemplate, LlmProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = deepSeekRestTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> requestNarrative(String systemPrompt, String userPrompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("holdhive.llm.api-key is not set - skipping DeepSeek call and returning facts-only result");
            return Optional.empty();
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", properties.model(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.3);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.apiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    properties.baseUrl() + "/chat/completions", entity, Map.class);

            HttpStatusCode status = response.getStatusCode();
            if (!status.is2xxSuccessful() || response.getBody() == null) {
                log.warn("DeepSeek call returned non-success status {} - falling back to facts-only result", status);
                return Optional.empty();
            }

            String content = extractMessageContent(response.getBody());
            if (content == null || content.isBlank()) {
                log.warn("DeepSeek response had no message content - falling back to facts-only result");
                return Optional.empty();
            }

            return Optional.of(objectMapper.readTree(content));
        } catch (RestClientException e) {
            log.warn("DeepSeek call failed ({}) - falling back to facts-only result", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to parse DeepSeek response - falling back to facts-only result", e);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageContent(Map<?, ?> body) {
        Object choicesObj = body.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return null;
        }
        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object contentObj = messageMap.get("content");
        return contentObj == null ? null : contentObj.toString();
    }

    /**
     * Streams a narrative from DeepSeek using its OpenAI-compatible
     * {@code stream: true} chat completions mode instead of a single blocking
     * response. Deliberately uses the JDK's {@link HttpClient} (rather than
     * {@link RestTemplate}, which does not expose a streamed response body)
     * so no new reactive/WebFlux dependency is required.
     *
     * <p>Contract: {@code onToken} is invoked once per non-empty text chunk,
     * in order; exactly one of {@code onComplete} or {@code onError} is
     * invoked exactly once when the stream ends. If the API key is missing,
     * {@code onToken} receives a single explanatory fallback chunk and
     * {@code onComplete} is invoked - mirroring {@link #requestNarrative}'s
     * facts-only degradation instead of leaving the caller hanging.
     */
    public void streamNarrative(
            String systemPrompt,
            String userPrompt,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("holdhive.llm.api-key is not set - skipping DeepSeek stream and returning a fallback message");
            onToken.accept("AI commentary is not configured. Only structured data is shown below.");
            onComplete.run();
            return;
        }

        long timeoutMs = properties.timeoutMs() > 0 ? properties.timeoutMs() : 60_000L;
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", properties.model(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)),
                    "stream", true,
                    "temperature", 0.3);

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<java.util.stream.Stream<String>> response =
                    httpClient.send(httpRequest, BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                onError.accept(new IllegalStateException("DeepSeek stream call returned HTTP " + response.statusCode()));
                return;
            }

            response.body().forEach(line -> handleStreamLine(line, onToken));
            onComplete.run();
        } catch (Exception e) {
            log.warn("DeepSeek streaming call failed: {}", e.getMessage());
            onError.accept(e);
        }
    }

    private void handleStreamLine(String line, Consumer<String> onToken) {
        if (line == null || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring("data:".length()).trim();
        if (data.isEmpty() || data.equals("[DONE]")) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode delta = node.at("/choices/0/delta/content");
            if (!delta.isMissingNode() && delta.isTextual() && !delta.asText().isEmpty()) {
                onToken.accept(delta.asText());
            }
        } catch (Exception e) {
            log.warn("Failed to parse DeepSeek stream chunk, skipping: {}", e.getMessage());
        }
    }
}