package com.app.decisioniq.application.interpretation;

import com.app.decisioniq.application.assistant.InterpretationInput;
import com.app.decisioniq.application.catalog.SupportedCatalogUnit;
import com.app.decisioniq.application.catalog.UnsupportedCatalogReason;
import com.app.decisioniq.application.catalog.UnsupportedCatalogUnit;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterpretationValidatorTest {

    private final InterpretationValidator validator = new InterpretationValidator();

    @Test
    void acceptsCatalogSelectionFromRetrievedCandidates() {
        InterpretationInput input = input();
        QueryInterpretation interpretation = interpretation(
                supported("s0c0", List.of("MODEL_EVIDENCE")),
                ignored("s0c1")
        );

        assertThatCode(() -> validator.validate(input, interpretation)).doesNotThrowAnyException();
    }

    @Test
    void rejectsCatalogKeyNotRetrievedForTheUnit() {
        QueryInterpretation interpretation = interpretation(
                supported("s0c0", List.of("UNAUTHORIZED_CATALOG")),
                ignored("s0c1")
        );

        assertThatThrownBy(() -> validator.validate(input(), interpretation))
                .isInstanceOf(InvalidInterpretationException.class)
                .hasMessageContaining("not retrieved");
    }

    @Test
    void rejectsPromotionOfUnsupportedUnitWithoutCatalogAuthority() {
        QueryInterpretation interpretation = interpretation(
                supported("s0c0", List.of("MODEL_EVIDENCE")),
                supported("s0c1", List.of("MODEL_EVIDENCE"))
        );

        assertThatThrownBy(() -> validator.validate(input(), interpretation))
                .isInstanceOf(InvalidInterpretationException.class)
                .hasMessageContaining("without catalog authority");
    }

    @Test
    void rejectsContextReferenceToUnknownOrLaterUnit() {
        InterpretedUnit invalidContext = new InterpretedUnit(
                "s0c0",
                UnitDisposition.SUPPORTED_REQUEST,
                InterpretationOperation.LOOKUP,
                List.of("MODEL_EVIDENCE"),
                "s0c1"
        );

        assertThatThrownBy(() -> validator.validate(
                input(), interpretation(invalidContext, ignored("s0c1"))
        ))
                .isInstanceOf(InvalidInterpretationException.class)
                .hasMessageContaining("earlier source unit");
    }

    @Test
    void rejectsUnitsReturnedOutOfSourceOrder() {
        assertThatThrownBy(() -> validator.validate(
                input(), interpretation(ignored("s0c1"), supported("s0c0", List.of("MODEL_EVIDENCE")))
        ))
                .isInstanceOf(InvalidInterpretationException.class)
                .hasMessageContaining("source unit order");
    }

    private InterpretationInput input() {
        return new InterpretationInput(
                "1.1",
                "give me the model score and sing a song",
                List.of(new SupportedCatalogUnit(
                        frame(0, "give me the model score"),
                        List.of(new CatalogCandidate("MODEL_EVIDENCE", 2, 0.73))
                )),
                List.of(new UnsupportedCatalogUnit(
                        frame(1, "sing a song"),
                        UnsupportedCatalogReason.NO_CAPABILITY_MATCH,
                        0.08
                ))
        );
    }

    private QueryInterpretation interpretation(InterpretedUnit... units) {
        return new QueryInterpretation(
                "1.0",
                InterpretationDisposition.READY_FOR_PLANNING,
                List.of(units),
                List.of(),
                "gpt-5-mini",
                "prompt-v1",
                "response-1"
        );
    }

    private InterpretedUnit supported(String id, List<String> catalogKeys) {
        return new InterpretedUnit(
                id,
                UnitDisposition.SUPPORTED_REQUEST,
                InterpretationOperation.LOOKUP,
                catalogKeys,
                "NONE"
        );
    }

    private InterpretedUnit ignored(String id) {
        return new InterpretedUnit(
                id,
                UnitDisposition.IGNORE_NOISE,
                InterpretationOperation.NONE,
                List.of(),
                "NONE"
        );
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
