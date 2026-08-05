package com.app.decisioniq.domain.guardrail;

import java.util.Objects;

public record GuardrailDecision(
        GuardrailOutcome outcome,
        String normalizedQuestion,
        GuardrailReasonCode reasonCode,
        String auditFingerprint
) {

    public GuardrailDecision {
        Objects.requireNonNull(outcome, "outcome is required");
        Objects.requireNonNull(normalizedQuestion, "normalizedQuestion is required");
        Objects.requireNonNull(reasonCode, "reasonCode is required");
        Objects.requireNonNull(auditFingerprint, "auditFingerprint is required");
    }
}
