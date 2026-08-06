package com.app.decisioniq.config.guardrail;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class GuardrailConfigurationValidator {

    private static final Set<GuardrailProperties.DetectorId> REQUIRED_SECURITY_DETECTORS = Set.of(
            GuardrailProperties.DetectorId.RAW_SQL,
            GuardrailProperties.DetectorId.SCRIPT_ATTACK,
            GuardrailProperties.DetectorId.INSTRUCTION_BYPASS
    );

    private final GuardrailProperties properties;

    public GuardrailConfigurationValidator(GuardrailProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validateConfiguration() {
        requireUniquePatternIds("raw SQL patterns", properties.patterns().detectors().rawSql());
        requireUniquePatternIds("script attack patterns", properties.patterns().detectors().scriptAttack());
        requireUniquePatternIds(
                "instruction bypass patterns",
                properties.patterns().detectors().instructionBypass()
        );
        requireUnique("enabled detectors", properties.detectors().enabled());

        if (!new HashSet<>(properties.detectors().enabled()).containsAll(REQUIRED_SECURITY_DETECTORS)) {
            throw new IllegalStateException("All mandatory guardrail security detectors must be enabled");
        }
        List<String> configuredResponseCodes = List.of(
                properties.responses().blockedUnsupportedContent().code(),
                properties.responses().requestInvalid().code(),
                properties.responses().interpretationNotImplemented().code(),
                properties.responses().unsupportedMediaType().code(),
                properties.responses().internalError().code()
        );
        if (new HashSet<>(configuredResponseCodes).size() != configuredResponseCodes.size()) {
            throw new IllegalStateException("Guardrail response codes must be unique");
        }
    }

    private <T> void requireUnique(String name, List<T> values) {
        if (new HashSet<>(values).size() != values.size()) {
            throw new IllegalStateException("Guardrail " + name + " must not contain duplicates");
        }
    }

    private void requireUniquePatternIds(
            String name,
            List<GuardrailProperties.PatternDefinition> definitions
    ) {
        requireUnique(
                name,
                definitions.stream().map(GuardrailProperties.PatternDefinition::id).toList()
        );
    }
}
