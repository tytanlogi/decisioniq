package com.app.decisioniq.application.nlp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproducible 500-case stress matrix for the narrow pre-catalog clause-role contract.
 * It verifies role classification and same-message transaction-reference propagation only.
 */
@SpringBootTest
class NlpClauseRoleStressBenchmarkTest {

    @Autowired
    private NlpAnalyzer nlpAnalyzer;

    @Autowired
    private NlpOperationAnalyzer operationAnalyzer;

    @Autowired
    private NlpClauseRoleAnalyzer roleAnalyzer;

    @Test
    void passesTheFiveHundredCaseContextAndLookupStressMatrix() {
        List<BenchmarkCase> cases = cases();
        assertThat(cases).hasSize(500);
        assertThat(cases.stream().map(BenchmarkCase::question).collect(Collectors.toSet())).hasSize(500);

        List<String> failures = new ArrayList<>();
        for (BenchmarkCase testCase : cases) {
            List<NlpOperationFrame> frames = classify(testCase.question());
            List<NlpClauseRole> actualRoles = frames.stream().map(NlpOperationFrame::role).toList();
            if (!actualRoles.equals(testCase.expectedRoles())) {
                failures.add("category=" + testCase.category() + " expectedRoles="
                        + testCase.expectedRoles() + " actualRoles=" + actualRoles
                        + " question=" + testCase.question());
                continue;
            }
            if (testCase.expectedResolvedTransactionId() != null
                    && !testCase.expectedResolvedTransactionId().equals(frames.getLast().resolvedTransactionId())) {
                failures.add("category=" + testCase.category() + " expectedResolvedId="
                        + testCase.expectedResolvedTransactionId() + " actualResolvedId="
                        + frames.getLast().resolvedTransactionId() + " question=" + testCase.question());
            }
        }

        assertThat(failures)
                .as("Context/lookup stress failures:%n%s", String.join("%n", failures))
                .isEmpty();
    }

    private List<BenchmarkCase> cases() {
        List<BenchmarkCase> cases = new ArrayList<>();
        List<String> ids = ids(1, 25);
        List<String> contextTemplates = List.of(
                "%s was approved.", "Transaction %s was declined.", "%s was accepted.",
                "Transaction %s was rejected.", "%s was let through."
        );
        List<String> lookupTemplates = List.of(
                "What was the model score?", "Which rule fired?", "Show the transaction details.",
                "Tell me the risk band.", "Give me the decision reasons."
        );

        // 125: declarative context followed by a single lookup.
        for (int idIndex = 0; idIndex < ids.size(); idIndex++) {
            for (int templateIndex = 0; templateIndex < contextTemplates.size(); templateIndex++) {
                String id = ids.get(idIndex);
                String question = contextTemplates.get(templateIndex).formatted(id) + " "
                        + lookupTemplates.get((idIndex + templateIndex) % lookupTemplates.size());
                cases.add(twoUnit("context_then_lookup", question, id));
            }
        }

        // 100: explicit-ID questions must always remain executable.
        List<String> explicitQuestions = List.of(
                "Why was %s approved?", "Was %s approved?", "What was the model score for %s?",
                "Which rule fired for %s?", "Where did %s occur?", "Explain %s decision.",
                "Did %s get declined?", "What reasons were recorded for %s?"
        );
        for (String id : ids.subList(0, 20)) {
            for (String template : explicitQuestions.subList(0, 5)) {
                cases.add(oneUnit("explicit_question", template.formatted(id), NlpClauseRole.EXECUTABLE_REQUEST, id));
            }
        }

        // 75: imperatives, including polite wording, must never be discarded as context.
        List<String> imperatives = List.of(
                "Show %s details.", "Give me the model score for %s.", "Tell me why %s was approved.",
                "Please show %s details.", "Please tell me the risk band for %s."
        );
        for (String id : ids.subList(0, 15)) {
            for (String template : imperatives) {
                cases.add(oneUnit("imperative_request", template.formatted(id), NlpClauseRole.EXECUTABLE_REQUEST, id));
            }
        }

        // 50: context-only input is deliberately non-executable.
        for (String id : ids.subList(0, 10)) {
            for (String template : contextTemplates) {
                cases.add(oneUnit("context_only", template.formatted(id), NlpClauseRole.CONTEXT_STATEMENT, null));
            }
        }

        // 50: declarative text without a valid identifier must not be silently discarded.
        List<String> noIdentifier = List.of(
                "The transaction was approved", "The transaction was declined", "The payment was accepted",
                "The decision was rejected", "The case was approved"
        );
        for (int batch = 1; batch <= 10; batch++) {
            for (String statement : noIdentifier) {
                cases.add(oneUnit("unresolvable_statement", statement + " in review batch " + batch + ".",
                        NlpClauseRole.EXECUTABLE_REQUEST, null));
            }
        }

        // 50: one context statement can serve two following requests in the same message.
        for (String id : ids.subList(0, 10)) {
            for (int variant = 0; variant < 5; variant++) {
                String question = contextTemplates.get(variant).formatted(id)
                        + " What was the model score? Which rule fired?";
                cases.add(new BenchmarkCase("context_two_lookups", question,
                        List.of(NlpClauseRole.CONTEXT_STATEMENT, NlpClauseRole.EXECUTABLE_REQUEST,
                                NlpClauseRole.EXECUTABLE_REQUEST), id));
            }
        }

        // 25: questions with no identifier stay executable and must later be clarified, not dropped.
        List<String> missingReference = List.of(
                "What was the model score?", "Why was it approved?", "Which rule fired?",
                "Show the transaction details.", "Tell me the outcome."
        );
        for (int variant = 1; variant <= 5; variant++) {
            for (String question : missingReference) {
                String numberedQuestion = question.contains("?")
                        ? question.replace("?", " in query variant " + variant + "?")
                        : question.replace(".", " in query variant " + variant + ".");
                cases.add(oneUnit("missing_reference_question", numberedQuestion,
                        NlpClauseRole.EXECUTABLE_REQUEST, null));
            }
        }

        // 25: malformed IDs must not create context or silently resolve to a real identifier.
        List<String> malformed = List.of(
                "TXN 100001 was approved.", "TXN_100002 was declined.", "transaction was approved.",
                "TXN- was accepted.", "TXN-ABC was rejected."
        );
        for (int variant = 1; variant <= 5; variant++) {
            for (String question : malformed) {
                cases.add(oneUnit("malformed_identifier", question.replace(".", " in batch " + variant + "."),
                        NlpClauseRole.EXECUTABLE_REQUEST, null));
            }
        }

        return List.copyOf(cases);
    }

    private BenchmarkCase twoUnit(String category, String question, String resolvedId) {
        return new BenchmarkCase(category, question,
                List.of(NlpClauseRole.CONTEXT_STATEMENT, NlpClauseRole.EXECUTABLE_REQUEST), resolvedId);
    }

    private BenchmarkCase oneUnit(String category, String question, NlpClauseRole role, String resolvedId) {
        return new BenchmarkCase(category, question, List.of(role), resolvedId);
    }

    private List<String> ids(int fromInclusive, int toInclusive) {
        List<String> ids = new ArrayList<>();
        for (int number = fromInclusive; number <= toInclusive; number++) {
            String prefix = number % 2 == 0 ? "TX" : "TXN";
            ids.add(prefix + "-" + String.format("%06d", number));
        }
        return ids;
    }

    private List<NlpOperationFrame> classify(String question) {
        NlpAnalysis analysis = nlpAnalyzer.analyze(question);
        return roleAnalyzer.classify(analysis, operationAnalyzer.analyze(analysis));
    }

    private record BenchmarkCase(
            String category,
            String question,
            List<NlpClauseRole> expectedRoles,
            String expectedResolvedTransactionId
    ) { }
}
