package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.nlp.OperationPolicyOutcome;
import com.app.decisioniq.application.interpretation.QueryInterpretation;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;

import java.util.List;
import java.util.Objects;

public record AssistantRequestResult(
        String correlationId,
        String requestId,
        GuardrailDecision guardrailDecision,
        OperationPolicyOutcome operationPolicyOutcome,
        InterpretationInput interpretationInput,
        QueryInterpretation interpretation
) {

    public AssistantRequestResult {
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(requestId, "requestId is required");
        Objects.requireNonNull(guardrailDecision, "guardrailDecision is required");
        operationPolicyOutcome = operationPolicyOutcome == null
                ? OperationPolicyOutcome.NOT_EVALUATED
                : operationPolicyOutcome;
    }

}
