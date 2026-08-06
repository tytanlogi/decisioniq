package com.app.decisioniq.api.error;

import com.app.decisioniq.application.nlp.OperationPolicyOutcome;
import com.app.decisioniq.domain.catalog.CatalogRelevanceOutcome;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
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
        CatalogRelevanceOutcome catalogRelevanceOutcome,
        List<Match> catalogMatches,
        List<ApiFieldViolation> violations
) {

    public ApiErrorResponse {
        catalogMatches = List.copyOf(catalogMatches);
        violations = List.copyOf(violations);
    }
}
