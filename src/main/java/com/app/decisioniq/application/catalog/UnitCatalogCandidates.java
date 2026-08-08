package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogCandidate;

import java.util.List;

public record UnitCatalogCandidates(
        NlpOperationFrame unit,
        List<CatalogCandidate> candidates
) {
    public UnitCatalogCandidates {
        candidates = List.copyOf(candidates);
    }
}
