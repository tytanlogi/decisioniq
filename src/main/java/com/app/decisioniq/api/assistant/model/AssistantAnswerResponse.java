package com.app.decisioniq.api.assistant.model;

import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import com.app.decisioniq.application.interpretation.QueryInterpretation;

/**
 * Response payload consumed by the React assistant UI.
 */
public record AssistantAnswerResponse(
        String answer,
        String code,
        String correlationId,
        String requestId,
        GuardrailOutcome guardrailOutcome,
        GuardrailReasonCode guardrailReason,
        QueryInterpretation interpretation
) { }
