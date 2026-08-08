package com.app.decisioniq.infrastructure.llm.openai;

import com.app.decisioniq.application.interpretation.InterpretationDisposition;
import com.app.decisioniq.application.interpretation.InterpretationOperation;
import com.app.decisioniq.application.interpretation.UnitDisposition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiInterpretationMapperTest {

    private final OpenAiInterpretationMapper mapper = new OpenAiInterpretationMapper();

    @Test
    void suppliesNeutralPlanningValuesForIgnoredUnits() {
        OpenAiInterpretationOutput.Unit supported = unit(
                "s0c0",
                "SUPPORTED_REQUEST",
                "EXPLAIN",
                List.of("decision.outcome"),
                "NONE"
        );
        OpenAiInterpretationOutput.Unit ignored = unit(
                "s0c1",
                "IGNORE_NOISE",
                null,
                List.of(),
                null
        );

        OpenAiInterpretationOutput output = new OpenAiInterpretationOutput();
        output.disposition = "READY_FOR_PLANNING";
        output.units = List.of(supported, ignored);

        var interpretation = mapper.toDomain(output, "gpt-5-mini", "v1", "response-1");

        assertThat(interpretation.disposition())
                .isEqualTo(InterpretationDisposition.READY_FOR_PLANNING);
        assertThat(interpretation.units().get(1).disposition())
                .isEqualTo(UnitDisposition.IGNORE_NOISE);
        assertThat(interpretation.units().get(0).operation())
                .isEqualTo(InterpretationOperation.EXPLAIN);
        assertThat(interpretation.units().get(1).operation())
                .isEqualTo(InterpretationOperation.NONE);
        assertThat(interpretation.units().get(1).contextSourceUnitId()).isEqualTo("NONE");
    }

    private OpenAiInterpretationOutput.Unit unit(
            String sourceUnitId,
            String disposition,
            String operation,
            List<String> selectedCatalogKeys,
            String contextSourceUnitId
    ) {
        OpenAiInterpretationOutput.Unit unit = new OpenAiInterpretationOutput.Unit();
        unit.sourceUnitId = sourceUnitId;
        unit.disposition = disposition;
        unit.operation = operation;
        unit.selectedCatalogKeys = selectedCatalogKeys;
        unit.contextSourceUnitId = contextSourceUnitId;
        return unit;
    }
}
