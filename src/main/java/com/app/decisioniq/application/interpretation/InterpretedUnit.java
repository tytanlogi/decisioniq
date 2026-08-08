package com.app.decisioniq.application.interpretation;

import java.util.List;

public record InterpretedUnit(
        String sourceUnitId,
        UnitDisposition disposition,
        InterpretationOperation operation,
        List<String> selectedCatalogKeys,
        String contextSourceUnitId
) {
    public InterpretedUnit {
        selectedCatalogKeys = List.copyOf(selectedCatalogKeys);
    }
}
