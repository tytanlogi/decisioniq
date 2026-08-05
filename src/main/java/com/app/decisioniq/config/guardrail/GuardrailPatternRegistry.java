package com.app.decisioniq.config.guardrail;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Component
public class GuardrailPatternRegistry {

    private static final String ACTIONS_TOKEN = "{{actions}}";
    private static final String OBJECTS_TOKEN = "{{objects}}";
    private final Pattern correlationId;
    private final Map<GuardrailProperties.DetectorId, List<Pattern>> detectorPatterns;
    private final Pattern mutationDomainObject;

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

        String actions = alternatives(properties.vocabulary().mutationAliases());
        String objects = alternatives(properties.vocabulary().mutationDomainObjects());
        this.mutationDomainObject = compileTemplate(
                "mutation-domain-object",
                properties.patterns().mutation().domainObjectTemplate(),
                actions,
                objects
        );
    }

    public Pattern correlationId() {
        return correlationId;
    }

    public List<Pattern> detectorPatterns(GuardrailProperties.DetectorId detectorId) {
        return detectorPatterns.getOrDefault(detectorId, List.of());
    }

    public Pattern mutationDomainObject() {
        return mutationDomainObject;
    }

    private List<Pattern> compileDefinitions(List<GuardrailProperties.PatternDefinition> definitions) {
        return definitions.stream()
                .map(definition -> compile(definition.id(), definition.expression()))
                .toList();
    }

    private Pattern compileTemplate(
            String id,
            String template,
            String actions,
            String objects
    ) {
        if (!template.contains(ACTIONS_TOKEN)) {
            throw new IllegalStateException("Guardrail pattern " + id + " must contain " + ACTIONS_TOKEN);
        }
        String expression = template
                .replace(ACTIONS_TOKEN, actions)
                .replace(OBJECTS_TOKEN, objects);
        return compile(id, expression);
    }

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

    private String alternatives(List<String> values) {
        return values.stream()
                .map(String::strip)
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
    }
}
