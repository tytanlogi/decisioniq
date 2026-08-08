package com.app.decisioniq.domain.guardrail;

import java.util.Objects;

public record GuardrailDecision(
        GuardrailOutcome outcome,
        String normalizedQuestion,
        GuardrailReasonCode reasonCode
) {

    /**
     * Ensures every guardrail decision is complete before it can leave the domain boundary.
     */
    public GuardrailDecision {
        Objects.requireNonNull(outcome, "outcome is required");
        Objects.requireNonNull(normalizedQuestion, "normalizedQuestion is required");
        Objects.requireNonNull(reasonCode, "reasonCode is required");
    }
}
