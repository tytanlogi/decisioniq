package com.app.decisioniq.infrastructure.llm.openai;

import com.app.decisioniq.application.interpretation.InterpretationDisposition;
import com.app.decisioniq.application.interpretation.InterpretationOperation;
import com.app.decisioniq.application.interpretation.InterpretedUnit;
import com.app.decisioniq.application.interpretation.QueryInterpretation;
import com.app.decisioniq.application.interpretation.UnitDisposition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalContentMaskerTest {

    private final ExternalContentMasker masker = new ExternalContentMasker();

    @Test
    void masksBusinessIdentifiersAndRestoresClarificationText() {
        ExternalContentMasker.MaskedContent masked = masker.mask(
                "Explain TXN-006451 for CUST-0001 correlation "
                        + "ed314737-abf0-409e-8894-83865963ae14"
        );

        assertThat(masked.value())
                .doesNotContain("TXN-006451", "CUST-0001", "ed314737-abf0-409e-8894-83865963ae14")
                .contains("DIQ_ID_1", "DIQ_ID_2", "DIQ_ID_3");

        QueryInterpretation restored = masked.restore(new QueryInterpretation(
                "1.0",
                InterpretationDisposition.CLARIFICATION_REQUIRED,
                List.of(new InterpretedUnit(
                        "s0c0",
                        UnitDisposition.CLARIFY,
                        InterpretationOperation.NONE,
                        List.of(),
                        "NONE"
                )),
                List.of("Which transaction is DIQ_ID_1?"),
                "gpt-5-mini",
                "prompt-v1",
                "response-1"
        ));

        assertThat(restored.clarificationQuestions()).containsExactly(
                "Which transaction is TXN-006451?"
        );
    }
}
