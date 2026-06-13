package com.app.decisioniq.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OPenAiProperties(
        String baseUrl,
        String apiKey,
        String model
) { }
