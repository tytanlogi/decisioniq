package com.app.decisioniq.application.nlp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NlpAnalyzerGoldenTest {

    @Autowired
    private NlpAnalyzer analyzer;

    @Test
    void producesExpectedClauseBoundariesForRepresentativeQuestions() {
        List<GoldenCase> cases = List.of(
                one("Why was TXN-006451 approved?"),
                one("Give me the amount and currency for TXN-006451."),
                golden(
                        "Why was TXN-006451 approved and what was its model score?",
                        "Why was TXN-006451 approved",
                        "what was its model score?"
                ),
                golden(
                        "yummy the tummy and Why was TXN-006451 approved?",
                        "yummy the tummy",
                        "Why was TXN-006451 approved?"
                ),
                golden(
                        "Why was TXN-006451 approved and yummy the tummy?",
                        "Why was TXN-006451 approved",
                        "yummy the tummy?"
                ),
                golden(
                        "Why was transaction 123 approved and email that to me?",
                        "Why was transaction 123 approved",
                        "email that to me?"
                ),
                golden(
                        "why was transaction tx:123232 approved and if it is then fuck off",
                        "why was transaction tx:123232 approved",
                        "if it is then fuck off"
                ),
                golden(
                        "Show TXN-006451; explain its model score.",
                        "Show TXN-006451",
                        "explain its model score."
                ),
                golden(
                        "Show TXN-006451. What was its model score?",
                        "Show TXN-006451.",
                        "What was its model score?"
                ),
                one("Show transactions in Dubai with amount and currency."),
                one("Show transactions over 100 and under 500 dollars."),
                one("Create a table with transaction amount and model score."),
                golden(
                        "Explain TXN-006451 and then delete transaction TXN-006452.",
                        "Explain TXN-006451",
                        "then delete transaction TXN-006452."
                )
        );

        for (GoldenCase testCase : cases) {
            List<String> clauses = analyzer.analyze(testCase.question()).sentences().stream()
                    .flatMap(sentence -> sentence.clauses().stream())
                    .map(NlpAnalysis.Clause::text)
                    .toList();

            assertThat(clauses)
                    .as(testCase.question())
                    .containsExactlyElementsOf(testCase.expectedClauses());
        }
    }

    @Test
    void exposesTokensLemmasTagsDependenciesAndOriginalSpans() {
        String question = "Show transactions above $100 in Dubai during the last 24 hours.";

        NlpAnalysis analysis = analyzer.analyze(question);
        NlpAnalysis.Sentence sentence = analysis.sentences().getFirst();

        assertThat(sentence.tokens())
                .anySatisfy(token -> {
                    assertThat(token.text()).isEqualTo("transactions");
                    assertThat(token.lemma()).isEqualTo("transaction");
                    assertThat(token.partOfSpeech()).isEqualTo("NNS");
                });
        assertThat(sentence.dependencies()).isNotEmpty();
        assertThat(sentence.tokens()).allSatisfy(token ->
                assertThat(question.substring(token.begin(), token.end())).isEqualTo(token.text())
        );
    }

    @Test
    void preservesAmbiguousDomainTextWithoutSilentlyCorrectingIt() {
        NlpAnalysis analysis = analyzer.analyze(
                "Why was transaction 123 approved and EMI that to me?"
        );

        assertThat(analysis.sentences().getFirst().tokens())
                .extracting(NlpAnalysis.Token::text)
                .contains("EMI");
        assertThat(analysis.sentences().getFirst().clauses())
                .extracting(NlpAnalysis.Clause::text)
                .containsExactly(
                        "Why was transaction 123 approved",
                        "EMI that to me?"
                );
    }

    @Test
    void recognizesSentenceBoundaryWithoutWhitespaceAndPreservesOffsets() {
        String question = "Why was TXN-006451 approved.Create a report showing its score";

        NlpAnalysis analysis = analyzer.analyze(question);

        assertThat(analysis.sentences())
                .extracting(NlpAnalysis.Sentence::text)
                .containsExactly(
                        "Why was TXN-006451 approved.",
                        "Create a report showing its score"
                );
        assertThat(analysis.sentences()).allSatisfy(sentence ->
                assertThat(question.substring(sentence.begin(), sentence.end()))
                        .isEqualTo(sentence.text())
        );
    }

    private GoldenCase one(String question) {
        return golden(question, question);
    }

    private GoldenCase golden(String question, String... clauses) {
        return new GoldenCase(question, List.of(clauses));
    }

    private record GoldenCase(String question, List<String> expectedClauses) { }
}
