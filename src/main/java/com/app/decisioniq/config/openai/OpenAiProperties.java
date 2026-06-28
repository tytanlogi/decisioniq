package com.app.decisioniq.config.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String baseUrl,
        String apiKey,
        String model
) {
}
