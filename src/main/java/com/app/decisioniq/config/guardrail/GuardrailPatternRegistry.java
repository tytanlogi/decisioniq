package com.app.decisioniq.config.guardrail;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class GuardrailPatternRegistry {

    private final Pattern correlationId;
    private final Map<GuardrailProperties.DetectorId, List<Pattern>> detectorPatterns;

    /**
     * Compiles all configured regex definitions once so request processing reuses safe patterns.
     */
    public GuardrailPatternRegistry(GuardrailProperties properties) {
        this.correlationId = compile("correlation-id", properties.patterns().correlationId());

        this.detectorPatterns = new EnumMap<>(GuardrailProperties.DetectorId.class);
        detectorPatterns.put(
                GuardrailProperties.DetectorId.RAW_SQL,
                compileDefinitions(properties.patterns().detectors().rawSql())
        );
        detectorPatterns.put(
                GuardrailProperties.DetectorId.SCRIPT_ATTACK,
                compileDefinitions(properties.patterns().detectors().scriptAttack())
        );
        detectorPatterns.put(
                GuardrailProperties.DetectorId.INSTRUCTION_BYPASS,
                compileDefinitions(properties.patterns().detectors().instructionBypass())
        );
    }

    /**
     * Returns the compiled pattern used to validate correlation identifiers.
     */
    public Pattern correlationId() {
        return correlationId;
    }

    /**
     * Returns the immutable compiled signatures associated with one detector type.
     */
    public List<Pattern> detectorPatterns(GuardrailProperties.DetectorId detectorId) {
        return detectorPatterns.getOrDefault(detectorId, List.of());
    }

    /**
     * Compiles a detector's versioned pattern definitions into reusable regex patterns.
     */
    private List<Pattern> compileDefinitions(List<GuardrailProperties.PatternDefinition> definitions) {
        return definitions.stream()
                .map(definition -> compile(definition.id(), definition.expression()))
                .toList();
    }

    /**
     * Rejects unsupported regex constructs and compiles a named pattern with a clear startup error.
     */
    private Pattern compile(String id, String expression) {
        if (containsNumericBackReference(expression)
                || expression.contains("(?<=")
                || expression.contains("(?<!")) {
            throw new IllegalStateException(
                    "Guardrail pattern " + id + " uses an unsupported regex construct"
            );
        }
        try {
            return Pattern.compile(expression);
        } catch (PatternSyntaxException exception) {
            throw new IllegalStateException("Invalid guardrail pattern " + id, exception);
        }
    }

    /**
     * Detects numeric backreferences, which are excluded from externally configured patterns.
     */
    private boolean containsNumericBackReference(String expression) {
        for (int index = 0; index < expression.length() - 1; index++) {
            if (expression.charAt(index) == '\\'
                    && expression.charAt(index + 1) >= '1'
                    && expression.charAt(index + 1) <= '9') {
                return true;
            }
        }
        return false;
    }
}
