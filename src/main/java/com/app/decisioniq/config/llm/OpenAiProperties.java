package com.app.decisioniq.config.llm;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "decisioniq.llm.openai")
public record OpenAiProperties(
        boolean enabled,
        String apiKey,
        @NotBlank String model,
        @NotNull Duration timeout,
        @Min(0) @Max(5) int maxRetries,
        @Min(256) @Max(10_000) long maxOutputTokens,
        @NotBlank String promptVersion,
        boolean allowExternalContent
) {
    @AssertTrue(message = "OPENAI_API_KEY is required when OpenAI interpretation is enabled")
    public boolean isApiKeyConfiguredWhenEnabled() {
        return !enabled || (apiKey != null && !apiKey.isBlank());
    }

    @AssertTrue(message = "External model content must be explicitly enabled")
    public boolean isExternalContentExplicitlyAllowed() {
        return !enabled || allowExternalContent;
    }
}
