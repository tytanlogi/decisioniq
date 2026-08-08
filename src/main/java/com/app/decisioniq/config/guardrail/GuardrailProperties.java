package com.app.decisioniq.config.guardrail;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "decisioniq.guardrails")
public record GuardrailProperties(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9._-]{1,32}")
        String version,
        @NotNull @Valid Limits limits,
        @NotNull @Valid Normalization normalization,
        @NotNull @Valid Patterns patterns,
        @NotNull @Valid Detectors detectors,
        @NotNull @Valid Responses responses
) {

    public record Limits(
            @Min(1) @Max(8192) int maxQuestionCharacters,
            @Min(1) @Max(256) int maxTenantIdCharacters,
            @Min(1) @Max(256) int maxUserIdCharacters,
            @Min(1) @Max(256) int maxConversationIdCharacters,
            @Min(1) @Max(200) int maxDesignationCharacters
    ) {
    }

    public record Normalization(
            boolean collapseWhitespace,
            @Min(1) @Max(3) int repeatedPunctuationLimit,
            @Min(1) @Max(3) int ellipsisLimit
    ) {
    }

    public record Detectors(
            @NotEmpty @Size(max = 8)
            List<@NotNull DetectorId> enabled
    ) {

        /**
         * Copies the enabled detector list so bound policy configuration cannot be mutated later.
         */
        public Detectors {
            enabled = enabled == null ? null : List.copyOf(enabled);
        }
    }

    public record Patterns(
            @NotBlank @Size(max = 512) String correlationId,
            @NotNull @Valid DetectorPatterns detectors
    ) {
    }

    public record DetectorPatterns(
            @NotEmpty @Size(max = 32) List<@NotNull @Valid PatternDefinition> rawSql,
            @NotEmpty @Size(max = 16) List<@NotNull @Valid PatternDefinition> scriptAttack,
            @NotEmpty @Size(max = 32) List<@NotNull @Valid PatternDefinition> instructionBypass
    ) {

        /**
         * Copies all detector definitions so the active pattern policy remains immutable.
         */
        public DetectorPatterns {
            rawSql = rawSql == null ? null : List.copyOf(rawSql);
            scriptAttack = scriptAttack == null ? null : List.copyOf(scriptAttack);
            instructionBypass = instructionBypass == null ? null : List.copyOf(instructionBypass);
        }
    }

    public record PatternDefinition(
            @NotBlank @Pattern(regexp = "[a-z][a-z0-9.-]{1,63}") String id,
            @NotBlank @Size(max = 1500) String expression
    ) {
    }

    public enum DetectorId {
        RAW_SQL,
        SCRIPT_ATTACK,
        INSTRUCTION_BYPASS
    }

    public record Responses(
            @NotNull @Valid ResponseTemplate blockedUnsupportedContent,
            @NotNull @Valid ResponseTemplate requestInvalid,
            @NotNull @Valid ResponseTemplate interpretationNotImplemented,
            @NotNull @Valid ResponseTemplate unsupportedMediaType,
            @NotNull @Valid ResponseTemplate internalError
    ) {
    }

    public record ResponseTemplate(
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{2,63}") String code,
            @NotBlank @Size(max = 500) String message
    ) {
    }
}
