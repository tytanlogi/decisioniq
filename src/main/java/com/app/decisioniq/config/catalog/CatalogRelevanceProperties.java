package com.app.decisioniq.config.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "decisioniq.catalog-relevance")
public record CatalogRelevanceProperties(
        @NotBlank String baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @DecimalMin("-1.0") @DecimalMax("1.0") double supportedScore,
        @DecimalMin("-1.0") @DecimalMax("1.0") double ambiguousScore,
        @Min(1) @Max(10) int lowInformationTermLimit,
        @NotNull @Valid Responses responses
) {
    public CatalogRelevanceProperties {
        if (ambiguousScore >= supportedScore) {
            throw new IllegalArgumentException(
                    "Catalog ambiguous score must be lower than supported score"
            );
        }
        if (connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("Catalog client timeouts must be positive");
        }
    }

    public record Responses(
            @NotNull @Valid ResponseTemplate ambiguous,
            @NotNull @Valid ResponseTemplate outOfScope,
            @NotNull @Valid ResponseTemplate insufficientContext,
            @NotNull @Valid ResponseTemplate unavailable
    ) { }

    public record ResponseTemplate(
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{2,63}") String code,
            @NotBlank String message
    ) { }
}
