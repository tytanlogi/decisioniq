package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogCandidate;

import java.util.List;
import java.util.Objects;

public record SupportedCatalogUnit(
        NlpOperationFrame unit,
        List<CatalogCandidate> candidates
) {
    public SupportedCatalogUnit {
        Objects.requireNonNull(unit, "unit is required");
        candidates = List.copyOf(candidates);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
    }
}
