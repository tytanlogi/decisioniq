package com.app.decisioniq.application.nlp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NlpOperationAnalyzerTest {

    @Autowired
    private NlpAnalyzer nlpAnalyzer;

    @Autowired
    private NlpOperationAnalyzer operationAnalyzer;

    @ParameterizedTest
    @CsvSource(value = {
            "Why was TXN-006451 approved?|READ|false",
            "Approve transaction TXN-006451|MODIFY|true",
            "Create a table showing all transactions|PRESENT|false",
            "Create a table in the database|PERSIST|true",
            "Build a report summarizing transaction decisions|PRESENT|false",
            "Persist these results in the datastore|PERSIST|true",
            "Retrieve transaction records from another database|READ|false",
            "Materialize these results in the database|PERSIST|true",
            "Replicate transaction records into an archival store|PERSIST|true",
            "Copy all transaction data|TRANSFER|true",
            "Email this report to the analyst|EXTERNAL_ACTION|true",
            "Inspect all transactions|UNRESOLVED_ACTION|false"
    }, delimiter = '|')
    void classifiesCanonicalEffectsWithoutSentenceSpecificRules(
            String question,
            NlpOperationFrame.Effect expectedEffect,
            boolean blocked
    ) {
        NlpOperationFrame frame = operationAnalyzer.analyze(
                nlpAnalyzer.analyze(question)
        ).getFirst();

        assertThat(frame.effect()).isEqualTo(expectedEffect);
        assertThat(frame.blocksRequest()).isEqualTo(blocked);
    }

    @Test
    void exposesActionsObjectsTargetsAndCertainty() {
        NlpOperationFrame frame = operationAnalyzer.analyze(
                nlpAnalyzer.analyze("Create a table in the database")
        ).getFirst();

        assertThat(frame.actions()).contains("create");
        assertThat(frame.objects()).contains("table");
        assertThat(frame.targets()).contains("table", "database");
        assertThat(frame.certainty()).isEqualTo(NlpOperationFrame.Certainty.EXPLICIT);
    }
}
