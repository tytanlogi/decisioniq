package com.app.decisioniq.api.error;

import com.app.decisioniq.application.nlp.OperationPolicyOutcome;
import com.app.decisioniq.application.assistant.InterpretationInput;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String correlationId,
        String requestId,
        GuardrailOutcome guardrailOutcome,
        GuardrailReasonCode guardrailReason,
        OperationPolicyOutcome operationPolicyOutcome,
        InterpretationInput interpretationInput,
        List<ApiFieldViolation> violations
) {

    public ApiErrorResponse {
        violations = List.copyOf(violations);
    }
}
