package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogCandidate;
import com.app.decisioniq.domain.catalog.CatalogSelection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSelectionValidatorTest {

    private final CatalogSelectionValidator validator = new CatalogSelectionValidator();

    @Test
    void acceptsOnlyKeyAndVersionPairsReturnedByTheCatalog() {
        List<UnitCatalogCandidates> candidates = List.of(new UnitCatalogCandidates(
                safeUnit(),
                List.of(new CatalogCandidate("MODEL_EVIDENCE", 2, 0.48))
        ));

        CatalogSelectionValidation result = validator.validate(
                List.of(
                        new CatalogSelection("MODEL_EVIDENCE", 2),
                        new CatalogSelection("INVENTED_SECRET", 1)
                ),
                candidates
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.approved())
                .extracting(CatalogCandidate::catalogKey)
                .containsExactly("MODEL_EVIDENCE");
        assertThat(result.rejected())
                .containsExactly(new CatalogSelection("INVENTED_SECRET", 1));
    }

    @Test
    void rejectsARealKeyWhenTheVersionWasNotRetrieved() {
        List<UnitCatalogCandidates> candidates = List.of(new UnitCatalogCandidates(
                safeUnit(),
                List.of(new CatalogCandidate("MODEL_EVIDENCE", 2, 0.48))
        ));

        CatalogSelectionValidation result = validator.validate(
                List.of(new CatalogSelection("MODEL_EVIDENCE", 3)),
                candidates
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.approved()).isEmpty();
    }

    private NlpOperationFrame safeUnit() {
        return new NlpOperationFrame(
                0,
                0,
                "what was the model score?",
                List.of("be"),
                List.of(),
                List.of(),
                NlpOperationFrame.Effect.SAFE_CANDIDATE
        );
    }
}
