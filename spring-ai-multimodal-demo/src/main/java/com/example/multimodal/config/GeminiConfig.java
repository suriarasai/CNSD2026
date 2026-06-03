package com.example.multimodal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Builds a {@link RestClient} pre-configured for the Gemini API: base URL,
 * JSON content type, and the {@code x-goog-api-key} auth header pulled from
 * {@code GEMINI_API_KEY} (loaded from {@code .env} by spring-dotenv).
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    RestClient geminiRestClient(GeminiProperties props) {
        if (!StringUtils.hasText(props.getApiKey())) {
            throw new IllegalStateException(
                    "Missing Gemini API key. Copy .env.example to .env and set GEMINI_API_KEY=...");
        }
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("x-goog-api-key", props.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
