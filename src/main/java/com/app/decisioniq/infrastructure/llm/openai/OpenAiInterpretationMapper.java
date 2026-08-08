package com.app.decisioniq.infrastructure.llm.openai;

import com.app.decisioniq.application.interpretation.InterpretationDisposition;
import com.app.decisioniq.application.interpretation.InterpretationOperation;
import com.app.decisioniq.application.interpretation.InterpretedUnit;
import com.app.decisioniq.application.interpretation.InvalidInterpretationException;
import com.app.decisioniq.application.interpretation.QueryInterpretation;
import com.app.decisioniq.application.interpretation.UnitDisposition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiInterpretationMapper {

    public QueryInterpretation toDomain(
            OpenAiInterpretationOutput output,
            String model,
            String promptVersion,
            String providerResponseId
    ) {
        if (output == null) {
            throw new InvalidInterpretationException("The model returned no structured output");
        }
        try {
            return new QueryInterpretation(
                    "1.0",
                    InterpretationDisposition.valueOf(output.disposition),
                    safe(output.units).stream().map(this::toUnit).toList(),
                    safe(output.clarificationQuestions),
                    model,
                    promptVersion,
                    providerResponseId
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidInterpretationException(
                    "The model returned unsupported interpretation values"
            );
        }
    }

    private InterpretedUnit toUnit(OpenAiInterpretationOutput.Unit unit) {
        UnitDisposition disposition = UnitDisposition.valueOf(unit.disposition);
        return new InterpretedUnit(
                required(unit.sourceUnitId),
                disposition,
                InterpretationOperation.valueOf(defaultValue(unit.operation, "NONE")),
                safe(unit.selectedCatalogKeys),
                defaultValue(unit.contextSourceUnitId, "NONE")
        );
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required model value is missing");
        }
        return value;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
