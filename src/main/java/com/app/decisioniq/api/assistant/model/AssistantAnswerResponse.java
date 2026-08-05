package com.app.decisioniq.api.assistant.model;

import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
import com.app.decisioniq.domain.catalog.CatalogRelevanceOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;

import java.util.List;

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
        CatalogRelevanceOutcome catalogRelevanceOutcome,
        List<Match> catalogMatches
) {
    public AssistantAnswerResponse {
        catalogMatches = List.copyOf(catalogMatches);
    }
}
