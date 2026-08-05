package com.app.decisioniq.application.guardrail.detector;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.config.guardrail.GuardrailPatternRegistry;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class UnsupportedMutationDetector implements GuardrailDetector {

    private final Pattern mutationOnObject;

    public UnsupportedMutationDetector(GuardrailPatternRegistry patternRegistry) {
        this.mutationOnObject = patternRegistry.mutationDomainObject();
    }

    @Override
    public GuardrailProperties.DetectorId detectorId() {
        return GuardrailProperties.DetectorId.UNSUPPORTED_MUTATION;
    }

    @Override
    public Optional<GuardrailDetection> detect(String text) {
        return mutationOnObject.matcher(text).find()
                ? Optional.of(new GuardrailDetection(
                GuardrailOutcome.BLOCKED_UNSUPPORTED_MUTATION,
                GuardrailReasonCode.UNSUPPORTED_MUTATION
        ))
                : Optional.empty();
    }
}
