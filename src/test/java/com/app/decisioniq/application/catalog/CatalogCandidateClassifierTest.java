package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.config.catalog.CatalogClientProperties;
import com.app.decisioniq.domain.catalog.CatalogCandidate;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogCandidateClassifierTest {

    private final CatalogCandidateClassifier classifier = new CatalogCandidateClassifier(
            new CatalogClientProperties(
                    "http://localhost:8081",
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(3),
                    5,
                    0.28
            )
    );

    @Test
    void separatesSupportedUnitsFromNoCapabilityMatches() {
        NlpOperationFrame approval = unit(0, "why was TXN-006451 approved");
        NlpOperationFrame noise = unit(1, "go to hell");

        CatalogUnitPartition result = classifier.classify(List.of(
                new UnitCatalogCandidates(approval, List.of(
                        candidate("DECISION_EXPLANATION", 0.526),
                        candidate("RISK_RULE_EVIDENCE", 0.472)
                )),
                new UnitCatalogCandidates(noise, List.of(
                        candidate("TRANSACTION_SEARCH", -0.008),
                        candidate("RISK_RULE_EVIDENCE", -0.024)
                ))
        ));

        assertThat(result.supportedUnits()).singleElement().satisfies(supported -> {
            assertThat(supported.unit()).isEqualTo(approval);
            assertThat(supported.candidates())
                    .extracting(CatalogCandidate::catalogKey)
                    .containsExactly("DECISION_EXPLANATION", "RISK_RULE_EVIDENCE");
        });
        assertThat(result.unsupportedUnits()).singleElement().satisfies(unsupported -> {
            assertThat(unsupported.unit()).isEqualTo(noise);
            assertThat(unsupported.reason())
                    .isEqualTo(UnsupportedCatalogReason.NO_CAPABILITY_MATCH);
            assertThat(unsupported.bestScore()).isEqualTo(-0.008);
        });
    }

    @Test
    void retainsCloseHighScoringCandidatesForInterpretation() {
        NlpOperationFrame unit = unit(0, "show decision quality");

        CatalogUnitPartition result = classifier.classify(List.of(
                new UnitCatalogCandidates(unit, List.of(
                        candidate("DECISION_EXPLANATION", 0.44),
                        candidate("DECISION_QUALITY_ANALYSIS", 0.43)
                ))
        ));

        assertThat(result.unsupportedUnits()).isEmpty();
        assertThat(result.supportedUnits()).singleElement().satisfies(supported ->
                assertThat(supported.candidates())
                        .extracting(CatalogCandidate::catalogKey)
                        .containsExactly("DECISION_EXPLANATION", "DECISION_QUALITY_ANALYSIS")
        );
    }

    @Test
    void reportsEmptySearchResultsAsNoCapabilityMatch() {
        NlpOperationFrame unit = unit(0, "unrelated words");

        CatalogUnitPartition result = classifier.classify(List.of(
                new UnitCatalogCandidates(unit, List.of())
        ));

        assertThat(result.supportedUnits()).isEmpty();
        assertThat(result.unsupportedUnits()).singleElement().satisfies(unsupported -> {
            assertThat(unsupported.reason())
                    .isEqualTo(UnsupportedCatalogReason.NO_CAPABILITY_MATCH);
            assertThat(unsupported.bestScore()).isNull();
        });
    }

    private CatalogCandidate candidate(String key, double score) {
        return new CatalogCandidate(key, 1, score);
    }

    private NlpOperationFrame unit(int clauseIndex, String text) {
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
