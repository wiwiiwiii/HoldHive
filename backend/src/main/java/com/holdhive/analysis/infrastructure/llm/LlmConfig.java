package com.holdhive.analysis.infrastructure.llm;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.holdhive.analysis.infrastructure.eastmoney.FundHoldingsProperties;

@Configuration
@EnableConfigurationProperties({LlmProperties.class, FundHoldingsProperties.class})
public class LlmConfig {

    @Bean
    public RestTemplate deepSeekRestTemplate(RestTemplateBuilder builder, LlmProperties properties) {
        long timeoutMs = properties.timeoutMs() > 0 ? properties.timeoutMs() : 15000L;
        return builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
