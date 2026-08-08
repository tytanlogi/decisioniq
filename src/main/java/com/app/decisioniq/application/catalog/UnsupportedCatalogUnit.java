package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;

import java.util.Objects;

public record UnsupportedCatalogUnit(
        NlpOperationFrame unit,
        UnsupportedCatalogReason reason,
        Double bestScore
) {
    public UnsupportedCatalogUnit {
        Objects.requireNonNull(unit, "unit is required");
        Objects.requireNonNull(reason, "reason is required");
    }
}
