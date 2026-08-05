package com.app.decisioniq.application.catalog;

import com.app.decisioniq.config.catalog.CatalogRelevanceProperties;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
import com.app.decisioniq.domain.catalog.CatalogRelevanceOutcome;
import com.app.decisioniq.infrastructure.catalog.CatalogClient;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogRelevanceServiceTest {

    private final CatalogClient client = mock(CatalogClient.class);
    private final CatalogRelevanceService service = new CatalogRelevanceService(client, properties());

    @Test
    void supportsHighConfidenceMatch() {
        when(client.search("model score for TXN-1", "corr-1"))
                .thenReturn(result(0.48));

        assertThat(service.evaluate("model score for TXN-1", "corr-1").outcome())
                .isEqualTo(CatalogRelevanceOutcome.SUPPORTED);
    }

    @Test
    void marksMiddleScoreAsAmbiguous() {
        when(client.search("costly customer order", "corr-2"))
                .thenReturn(result(0.24));

        assertThat(service.evaluate("costly customer order", "corr-2").outcome())
                .isEqualTo(CatalogRelevanceOutcome.AMBIGUOUS);
    }

    @Test
    void rejectsLongLowScoreQuestionAsOutOfScope() {
        when(client.search("write a poem about summer holidays", "corr-3"))
                .thenReturn(result(0.10));

        assertThat(service.evaluate("write a poem about summer holidays", "corr-3").outcome())
                .isEqualTo(CatalogRelevanceOutcome.OUT_OF_SCOPE);
    }

    @Test
    void defersShortLowSignalQuestionToContextPhase() {
        when(client.search("yes", "corr-4")).thenReturn(result(0.08));

        assertThat(service.evaluate("yes", "corr-4").outcome())
                .isEqualTo(CatalogRelevanceOutcome.INSUFFICIENT_CONTEXT);
    }

    @Test
    void rejectsSymbolNoiseWhenNoCatalogMatches() {
        when(client.search("!!!@@@", "corr-5"))
                .thenReturn(List.of());

        assertThat(service.evaluate("!!!@@@", "corr-5").outcome())
                .isEqualTo(CatalogRelevanceOutcome.OUT_OF_SCOPE);
    }

    @Test
    void supportsRequestWhenOneUnitIsNoiseAndAnotherUnitIsSupported() {
        when(client.search("yummy the tummy", "corr-6")).thenReturn(result(0.08));
        when(client.search("Why was TXN-006451 approved?", "corr-6"))
                .thenReturn(List.of(new Match("DECISION_EXPLANATION", 1, 0.44)));

        CatalogRelevanceDecision decision = service.evaluateUnits(
                List.of("yummy the tummy", "Why was TXN-006451 approved?"),
                "corr-6"
        );

        assertThat(decision.outcome()).isEqualTo(CatalogRelevanceOutcome.SUPPORTED);
        assertThat(decision.matches())
                .extracting(Match::catalogKey)
                .containsExactly("DECISION_EXPLANATION");
    }

    @Test
    void rejectsRequestWhenEveryUnitIsOutOfScope() {
        when(client.search("summer poem", "corr-7")).thenReturn(result(0.08));
        when(client.search("photosynthesis in plants", "corr-7")).thenReturn(result(0.05));

        assertThat(service.evaluateUnits(
                List.of("summer poem", "photosynthesis in plants"),
                "corr-7"
        ).outcome()).isEqualTo(CatalogRelevanceOutcome.OUT_OF_SCOPE);
    }

    @Test
    void meaningfulOutOfScopeUnitIsNotHiddenBySupportedUnit() {
        NlpOperationFrame valid = frame(
                "Why was TXN-1 approved?", NlpOperationFrame.Effect.READ
        );
        NlpOperationFrame unrelated = frame(
                "Explain photosynthesis", NlpOperationFrame.Effect.READ
        );
        when(client.search(valid.text(), "corr-8")).thenReturn(result(0.44));
        when(client.search(unrelated.text(), "corr-8")).thenReturn(result(0.05));

        assertThat(service.evaluateFrames(List.of(valid, unrelated), "corr-8").outcome())
                .isEqualTo(CatalogRelevanceOutcome.OUT_OF_SCOPE);
    }

    @Test
    void unsupportedUnknownFragmentCanBeIgnoredWhenAnotherUnitIsSupported() {
        NlpOperationFrame noise = frame(
                "yummy the tummy", NlpOperationFrame.Effect.UNKNOWN
        );
        NlpOperationFrame valid = frame(
                "Why was TXN-1 approved?", NlpOperationFrame.Effect.READ
        );
        when(client.search(noise.text(), "corr-9")).thenReturn(result(0.05));
        when(client.search(valid.text(), "corr-9")).thenReturn(result(0.44));

        assertThat(service.evaluateFrames(List.of(noise, valid), "corr-9").outcome())
                .isEqualTo(CatalogRelevanceOutcome.SUPPORTED);
    }

    @Test
    void buildsEffectiveQuestionFromSupportedUnitsOnly() {
        NlpOperationFrame valid = frame(
                "why was transaction tx:123232 approved", NlpOperationFrame.Effect.READ
        );
        NlpOperationFrame noise = frame(
                "if it is then fuck off", NlpOperationFrame.Effect.UNKNOWN
        );
        when(client.search(valid.text(), "corr-10")).thenReturn(result(0.44));
        when(client.search(noise.text(), "corr-10")).thenReturn(result(0.05));

        CatalogFrameValidation validation = service.validateFrames(
                List.of(valid, noise), "corr-10"
        );

        assertThat(validation.decision().outcome())
                .isEqualTo(CatalogRelevanceOutcome.SUPPORTED);
        assertThat(validation.effectiveQuestion()).isEqualTo(valid.text());
        assertThat(validation.units())
                .extracting(CatalogFrameValidation.UnitValidation::includedInEffectiveQuestion)
                .containsExactly(true, false);
    }

    private NlpOperationFrame frame(String text, NlpOperationFrame.Effect effect) {
        return new NlpOperationFrame(
                0, 0, text, List.of(), List.of(), List.of(), effect,
                NlpOperationFrame.Certainty.EXPLICIT
        );
    }

    private List<Match> result(double score) {
        return List.of(new Match("MODEL_EVIDENCE", 1, score));
    }

    private CatalogRelevanceProperties properties() {
        CatalogRelevanceProperties.ResponseTemplate template =
                new CatalogRelevanceProperties.ResponseTemplate("TEST_RESPONSE", "test");
        return new CatalogRelevanceProperties(
                "http://localhost:8081",
                Duration.ofSeconds(1),
                Duration.ofSeconds(3),
                0.30,
                0.18,
                2,
                new CatalogRelevanceProperties.Responses(template, template, template, template)
        );
    }
}
