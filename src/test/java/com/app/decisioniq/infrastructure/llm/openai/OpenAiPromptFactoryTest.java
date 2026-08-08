package com.app.decisioniq.infrastructure.llm.openai;

import com.app.decisioniq.application.assistant.InterpretationInput;
import com.app.decisioniq.application.catalog.SupportedCatalogUnit;
import com.app.decisioniq.application.catalog.UnsupportedCatalogReason;
import com.app.decisioniq.application.catalog.UnsupportedCatalogUnit;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiPromptFactoryTest {

    private final OpenAiPromptFactory factory = new OpenAiPromptFactory(new ObjectMapper());

    @Test
    void emitsOneOrderedCompactUnitListWithoutRetrievalMetadata() {
        InterpretationInput input = new InterpretationInput(
                "1.1",
                "explain the decision and sing a song",
                List.of(new SupportedCatalogUnit(
                        frame(0, "explain the decision"),
                        List.of(new CatalogCandidate("DECISION_EXPLANATION", 7, 0.82))
                )),
                List.of(new UnsupportedCatalogUnit(
                        frame(1, "sing a song"),
                        UnsupportedCatalogReason.NO_CAPABILITY_MATCH,
                        0.04
                ))
        );

        String prompt = factory.create(input);

        assertThat(prompt)
                .contains("\"units\"")
                .contains("\"unitId\":\"s0c0\"")
                .contains("\"unitId\":\"s0c1\"")
                .contains("\"candidateKeys\":[\"DECISION_EXPLANATION\"]")
                .doesNotContain(
                        "supportedUnits",
                        "unsupportedUnits",
                        "actions",
                        "objects",
                        "targets",
                        "version",
                        "score",
                        "bestScore"
                );
        assertThat(prompt.indexOf("s0c0")).isLessThan(prompt.indexOf("s0c1"));
    }

    private NlpOperationFrame frame(int clauseIndex, String text) {
        return new NlpOperationFrame(
                0,
                clauseIndex,
                text,
                List.of(),
                List.of(),
                List.of(),
                NlpOperationFrame.Effect.SAFE_CANDIDATE
        );
    }
}
