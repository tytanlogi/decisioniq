package com.app.decisioniq.application.nlp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A deterministic, reproducible 360-question matrix for the pre-catalog clause-role boundary.
 * It deliberately exercises context statements, executable questions, imperatives, and statements
 * without a usable identifier. It is not a production accuracy claim or an end-to-end RAG benchmark.
 */
@SpringBootTest
class NlpClauseRoleBenchmarkTest {

    @Autowired
    private NlpAnalyzer nlpAnalyzer;

    @Autowired
    private NlpOperationAnalyzer operationAnalyzer;

    @Autowired
    private NlpClauseRoleAnalyzer roleAnalyzer;

    @Test
    void classifiesTheFrozenThreeHundredAndSixtyQuestionMatrix() {
        List<BenchmarkCase> cases = cases();
        assertThat(cases).hasSize(360);
        assertThat(cases.stream().map(BenchmarkCase::question).collect(java.util.stream.Collectors.toSet()))
                .hasSize(360);

        for (BenchmarkCase testCase : cases) {
            List<NlpOperationFrame> frames = classify(testCase.question());
            assertThat(frames)
                    .as(testCase.question())
                    .extracting(NlpOperationFrame::role)
                    .containsExactlyElementsOf(testCase.expectedRoles());
            if (testCase.expectedResolvedTransactionId() != null) {
                assertThat(frames.getLast().resolvedTransactionId())
                        .as(testCase.question())
                        .isEqualTo(testCase.expectedResolvedTransactionId());
            }
        }
    }

    private List<BenchmarkCase> cases() {
        List<BenchmarkCase> cases = new ArrayList<>();
        List<String> ids = List.of(
                "TXN-100001", "TXN-100002", "TXN-100003", "TXN-100004", "TXN-100005",
                "TX-100006", "TX-100007", "TX-100008", "TX-100009", "TX-100010"
        );
        List<String> contextTemplates = List.of(
                "%s was approved.",
                "%s was declined.",
                "Transaction %s was approved.",
                "Transaction %s was rejected.",
                "%s was accepted.",
                "%s was let through."
        );
        List<String> lookupTemplates = List.of(
                "What was the model score?",
                "Which rule fired?",
                "Show the transaction details.",
                "Tell me the risk band.",
                "What were the decision reasons?",
                "Give me the transaction amount."
        );

        // 120 context-plus-lookup messages: context stays out of catalog retrieval.
        for (String id : ids) {
            for (int i = 0; i < 2; i++) {
                for (String lookup : lookupTemplates) {
                    String question = contextTemplates.get(i).formatted(id) + " " + lookup;
                    cases.add(new BenchmarkCase(question,
                            List.of(NlpClauseRole.CONTEXT_STATEMENT, NlpClauseRole.EXECUTABLE_REQUEST), id));
                }
            }
        }

        // 120 explicit-ID questions: even passive/historical wording remains executable when asked.
        List<String> questionTemplates = List.of(
                "Why was %s approved?", "Was %s approved?", "What was the model score for %s?",
                "Which rule fired for %s?", "Show %s details.", "Explain %s decision.",
                "Did %s get declined?", "What reasons were recorded for %s?",
                "Give me the risk band for %s.", "When was %s processed?",
                "Where did %s occur?", "Describe %s outcome."
        );
        for (String id : ids) {
            for (String template : questionTemplates) {
                cases.add(new BenchmarkCase(template.formatted(id),
                        List.of(NlpClauseRole.EXECUTABLE_REQUEST), id));
            }
        }

        // 60 context-only messages: they establish local context but must not produce a catalog call.
        for (String id : ids) {
            for (String template : contextTemplates) {
                cases.add(new BenchmarkCase(template.formatted(id),
                        List.of(NlpClauseRole.CONTEXT_STATEMENT), null));
            }
        }

        // 60 unresolvable declarative messages: never silently discard ordinary text as context.
        List<String> noIdentifierStatements = List.of(
                "The transaction was approved.", "The transaction was declined.",
                "The payment was accepted.", "The decision was rejected.",
                "The case was approved.", "The transaction was let through."
        );
        for (int i = 0; i < 10; i++) {
            for (String statement : noIdentifierStatements) {
                cases.add(new BenchmarkCase(statement.replace(".", " in review batch %02d.".formatted(i + 1)),
                        List.of(NlpClauseRole.EXECUTABLE_REQUEST), null));
            }
        }
        return List.copyOf(cases);
    }

    private List<NlpOperationFrame> classify(String question) {
        NlpAnalysis analysis = nlpAnalyzer.analyze(question);
        return roleAnalyzer.classify(analysis, operationAnalyzer.analyze(analysis));
    }

    private record BenchmarkCase(
            String question,
            List<NlpClauseRole> expectedRoles,
            String expectedResolvedTransactionId
    ) { }
}
