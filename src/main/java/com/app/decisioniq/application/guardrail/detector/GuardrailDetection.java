package com.app.decisioniq.application.guardrail.detector;

import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;

public record GuardrailDetection(
        GuardrailOutcome outcome,
        GuardrailReasonCode reasonCode
) {
}
