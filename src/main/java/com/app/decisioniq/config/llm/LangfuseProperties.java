package com.app.decisioniq.config.llm;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "decisioniq.llm.langfuse")
public record LangfuseProperties(
        boolean enabled,
        String endpoint,
        String publicKey,
        String secretKey,
        boolean captureContent,
        @NotBlank String environment,
        @NotBlank String release
) {
    @AssertTrue(message = "Langfuse endpoint and API keys are required when Langfuse is enabled")
    public boolean isConnectionConfiguredWhenEnabled() {
        return !enabled
                || isPresent(endpoint)
                && isPresent(publicKey)
                && isPresent(secretKey);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
