package com.app.decisioniq.application.guardrail.detector;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.config.guardrail.GuardrailPatternRegistry;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class InstructionBypassDetector implements GuardrailDetector {

    private final List<Pattern> signatures;

    public InstructionBypassDetector(GuardrailPatternRegistry patternRegistry) {
        this.signatures = patternRegistry.detectorPatterns(
                GuardrailProperties.DetectorId.INSTRUCTION_BYPASS
        );
    }

    @Override
    public GuardrailProperties.DetectorId detectorId() {
        return GuardrailProperties.DetectorId.INSTRUCTION_BYPASS;
    }

    @Override
    public Optional<GuardrailDetection> detect(String text) {
        return signatures.stream().anyMatch(pattern -> pattern.matcher(text).find())
                ? Optional.of(new GuardrailDetection(
                GuardrailOutcome.BLOCKED_OBVIOUS_ATTACK_RAW_SQL,
                GuardrailReasonCode.OBVIOUS_INSTRUCTION_BYPASS
        ))
                : Optional.empty();
    }
}
