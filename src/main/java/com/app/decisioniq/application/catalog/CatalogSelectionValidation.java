package com.app.decisioniq.application.catalog;

import com.app.decisioniq.domain.catalog.CatalogCandidate;
import com.app.decisioniq.domain.catalog.CatalogSelection;

import java.util.List;

public record CatalogSelectionValidation(
        List<CatalogCandidate> approved,
        List<CatalogSelection> rejected
) {
    public CatalogSelectionValidation {
        approved = List.copyOf(approved);
        rejected = List.copyOf(rejected);
    }

    public boolean valid() {
        return rejected.isEmpty();
    }
}
