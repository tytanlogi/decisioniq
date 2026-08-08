package com.app.decisioniq.application.assistant;

import com.app.decisioniq.domain.guardrail.GuardrailDecision;

public interface AssistantRequestBoundary {

    /**
     * Applies request-boundary policy and returns the authoritative guardrail decision.
     */
    GuardrailDecision evaluate(AssistantRequestCommand command);
}
