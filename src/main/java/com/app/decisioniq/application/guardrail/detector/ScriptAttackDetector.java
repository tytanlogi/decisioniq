package com.app.decisioniq.application.guardrail.detector;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.config.guardrail.GuardrailPatternRegistry;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ScriptAttackDetector implements GuardrailDetector {

    private final List<Pattern> signatures;

    /**
     * Loads the precompiled executable-content signatures from the central registry.
     */
    public ScriptAttackDetector(GuardrailPatternRegistry patternRegistry) {
        this.signatures = patternRegistry.detectorPatterns(
                GuardrailProperties.DetectorId.SCRIPT_ATTACK
        );
    }

    /**
     * Returns the configuration identifier for script-attack detection.
     */
    @Override
    public GuardrailProperties.DetectorId detectorId() {
        return GuardrailProperties.DetectorId.SCRIPT_ATTACK;
    }

    /**
     * Blocks text containing configured script or executable-content signatures.
     */
    @Override
    public Optional<GuardrailDetection> detect(String text) {
        return signatures.stream().anyMatch(pattern -> pattern.matcher(text).find())
                ? Optional.of(new GuardrailDetection(
                GuardrailOutcome.BLOCKED_OBVIOUS_ATTACK_RAW_SQL,
                GuardrailReasonCode.OBVIOUS_SCRIPT_ATTACK
        ))
                : Optional.empty();
    }
}
