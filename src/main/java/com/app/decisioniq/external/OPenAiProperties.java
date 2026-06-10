package com.app.decisioniq.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "openai")
public record OPenAiProperties(
        String baseUrl,
        String apiKey,
        String model) { }
