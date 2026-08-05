package com.app.decisioniq.api.error;

import com.app.decisioniq.application.catalog.CatalogFrameValidation;
import com.app.decisioniq.application.nlp.NlpAnalysis;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
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
        CatalogRelevanceOutcome catalogRelevanceOutcome,
        List<Match> catalogMatches,
        NlpAnalysis nlpAnalysis,
        List<NlpOperationFrame> operationFrames,
        CatalogFrameValidation catalogValidation,
        String effectiveQuestion,
        List<ApiFieldViolation> violations
) {

    public ApiErrorResponse {
        catalogMatches = List.copyOf(catalogMatches);
        operationFrames = List.copyOf(operationFrames);
        violations = List.copyOf(violations);
    }
}
