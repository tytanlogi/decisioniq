package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;

import java.util.List;

/**
 * Deterministic, pre-Milvus diagnostic result. It deliberately contains no catalog or LLM result.
 */
public record PreCatalogAnalysisResult(
        PreCatalogDisposition disposition,
        GuardrailDecision guardrailDecision,
        List<NlpOperationFrame> units,
        List<PreCatalogSearchRequest> catalogSearchRequests,
        String clarificationReason
) {
    public PreCatalogAnalysisResult {
        units = List.copyOf(units);
        catalogSearchRequests = List.copyOf(catalogSearchRequests);
    }
}
