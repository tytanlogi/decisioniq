package com.app.decisioniq.application.interpretation;

import java.util.List;

public record QueryInterpretation(
        String schemaVersion,
        InterpretationDisposition disposition,
        List<InterpretedUnit> units,
        List<String> clarificationQuestions,
        String model,
        String promptVersion,
        String providerResponseId
) {
    public QueryInterpretation {
        units = List.copyOf(units);
        clarificationQuestions = List.copyOf(clarificationQuestions);
    }
}
