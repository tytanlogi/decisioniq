package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogCandidate;
import com.app.decisioniq.infrastructure.catalog.CatalogClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogCandidateRetrieverTest {

    private final CatalogClient client = mock(CatalogClient.class);
    private final CatalogCandidateRetriever retriever = new CatalogCandidateRetriever(client);

    @Test
    void retrievesCandidatesForEverySafeUnitWithoutApplyingScoreThresholds() {
        NlpOperationFrame first = unit(0, "why was TXN-1 approved?");
        NlpOperationFrame second = unit(1, "yummy the tummy");
        CatalogCandidate high = new CatalogCandidate("DECISION_EXPLANATION", 2, 0.48);
        CatalogCandidate low = new CatalogCandidate("TRANSACTION_DETAILS", 1, 0.05);
        when(client.search(first.text(), "corr-1")).thenReturn(List.of(high));
        when(client.search(second.text(), "corr-1")).thenReturn(List.of(low));

        List<UnitCatalogCandidates> result = retriever.retrieve(
                List.of(first, second), "corr-1"
        );

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().candidates()).containsExactly(high);
        assertThat(result.getLast().candidates()).containsExactly(low);
        verify(client).search(first.text(), "corr-1");
        verify(client).search(second.text(), "corr-1");
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
