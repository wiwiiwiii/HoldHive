package com.holdhive.analysis.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code holdhive.llm.*} keys from application.yml. All fields are
 * expected to be supplied via environment variables in real use
 * (DEEPSEEK_BASE_URL / DEEPSEEK_API_KEY / DEEPSEEK_MODEL) - no API key is
 * ever committed to the repo.
 *
 * @param baseUrl   DeepSeek-compatible chat completions API base, e.g. https://api.deepseek.com
 * @param apiKey    bearer token; blank/missing means the LLM step is skipped and the
 *                  API falls back to layered facts only (see {@link DeepSeekClient})
 * @param model     model name, e.g. "deepseek-chat"
 * @param timeoutMs connect/read timeout in milliseconds for the HTTP call
 */
@ConfigurationProperties(prefix = "holdhive.llm")
public record LlmProperties(String baseUrl, String apiKey, String model, long timeoutMs) {
}
