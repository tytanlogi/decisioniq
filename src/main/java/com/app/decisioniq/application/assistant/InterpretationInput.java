package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.catalog.SupportedCatalogUnit;
import com.app.decisioniq.application.catalog.UnsupportedCatalogUnit;

import java.util.List;

public record InterpretationInput(
        String schemaVersion,
        String question,
        List<SupportedCatalogUnit> supportedUnits,
        List<UnsupportedCatalogUnit> unsupportedUnits
) {
    public InterpretationInput {
        supportedUnits = List.copyOf(supportedUnits);
        unsupportedUnits = List.copyOf(unsupportedUnits);
    }
}
