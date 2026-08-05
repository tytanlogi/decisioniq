package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.catalog.CatalogFrameValidation;
import com.app.decisioniq.application.nlp.NlpAnalysis;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;

import java.util.List;
import java.util.Objects;

public record AssistantRequestResult(
        String correlationId,
        String requestId,
        GuardrailDecision guardrailDecision,
        CatalogRelevanceDecision catalogRelevanceDecision,
        NlpAnalysis nlpAnalysis,
        List<NlpOperationFrame> operationFrames,
        CatalogFrameValidation catalogValidation,
        String effectiveQuestion
) {

    public AssistantRequestResult {
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(requestId, "requestId is required");
        Objects.requireNonNull(guardrailDecision, "guardrailDecision is required");
        operationFrames = operationFrames == null ? List.of() : List.copyOf(operationFrames);
    }

    public AssistantRequestResult(
            String correlationId,
            String requestId,
            GuardrailDecision guardrailDecision,
            CatalogRelevanceDecision catalogRelevanceDecision
    ) {
        this(
                correlationId,
                requestId,
                guardrailDecision,
                catalogRelevanceDecision,
                null,
                List.of(),
                null,
                null
        );
    }
}
