package com.app.decisioniq.application.nlp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NlpClauseRoleAnalyzerTest {

    @Autowired
    private NlpAnalyzer nlpAnalyzer;

    @Autowired
    private NlpOperationAnalyzer operationAnalyzer;

    @Autowired
    private NlpClauseRoleAnalyzer roleAnalyzer;

    @Test
    void keepsContextOutOfSearchAndResolvesItForTheFollowingLookup() {
        List<NlpOperationFrame> frames = classify(
                "TXN-006451 was approved. What was the model score?"
        );

        assertThat(frames).hasSize(2);
        assertThat(frames.getFirst().role()).isEqualTo(NlpClauseRole.CONTEXT_STATEMENT);
        assertThat(frames.getFirst().contextCandidate().transactionId()).isEqualTo("TXN-006451");
        assertThat(frames.getFirst().contextCandidate().assertedOutcome()).isEqualTo("APPROVED");
        assertThat(frames.getFirst().executable()).isFalse();
        assertThat(frames.getLast().role()).isEqualTo(NlpClauseRole.EXECUTABLE_REQUEST);
        assertThat(frames.getLast().resolvedTransactionId()).isEqualTo("TXN-006451");
    }

    @Test
    void keepsQuestionsAndImperativeRequestsExecutableEvenWhenTheyContainAnIdentifier() {
        List<NlpOperationFrame> question = classify("Was TXN-006451 approved?");
        List<NlpOperationFrame> imperative = classify("Show TXN-006451 details.");

        assertThat(question).allSatisfy(frame ->
                assertThat(frame.role()).isEqualTo(NlpClauseRole.EXECUTABLE_REQUEST));
        assertThat(imperative).allSatisfy(frame ->
                assertThat(frame.role()).isEqualTo(NlpClauseRole.EXECUTABLE_REQUEST));
    }

    @Test
    void resolvesAnEarlierContextOnlyWithinTheSameMessage() {
        List<NlpOperationFrame> frames = classify(
                "Transaction TXN-006451 was declined. Which rule fired? Show the model score too."
        );

        assertThat(frames.getFirst().role()).isEqualTo(NlpClauseRole.CONTEXT_STATEMENT);
        assertThat(frames.stream().skip(1)).allSatisfy(frame -> {
            assertThat(frame.executable()).isTrue();
            assertThat(frame.resolvedTransactionId()).isEqualTo("TXN-006451");
        });
    }

    @Test
    void doesNotTreatADeclarativeClauseWithoutAResolvableIdentifierAsContext() {
        List<NlpOperationFrame> frames = classify("The transaction was approved.");

        assertThat(frames).singleElement().satisfies(frame ->
                assertThat(frame.role()).isEqualTo(NlpClauseRole.EXECUTABLE_REQUEST));
    }

    private List<NlpOperationFrame> classify(String question) {
        NlpAnalysis analysis = nlpAnalyzer.analyze(question);
        return roleAnalyzer.classify(analysis, operationAnalyzer.analyze(analysis));
    }
}
