package com.app.decisioniq.application.guardrail.detector;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.config.guardrail.GuardrailPatternRegistry;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RawSqlDetector implements GuardrailDetector {

    private final List<java.util.regex.Pattern> signatures;

    public RawSqlDetector(GuardrailPatternRegistry patternRegistry) {
        this.signatures = patternRegistry.detectorPatterns(GuardrailProperties.DetectorId.RAW_SQL);
    }

    @Override
    public GuardrailProperties.DetectorId detectorId() {
        return GuardrailProperties.DetectorId.RAW_SQL;
    }

    @Override
    public Optional<GuardrailDetection> detect(String text) {
        return signatures.stream().anyMatch(pattern -> pattern.matcher(text).find())
                ? Optional.of(new GuardrailDetection(
                GuardrailOutcome.BLOCKED_OBVIOUS_ATTACK_RAW_SQL,
                GuardrailReasonCode.OBVIOUS_RAW_SQL
        ))
                : Optional.empty();
    }
}
