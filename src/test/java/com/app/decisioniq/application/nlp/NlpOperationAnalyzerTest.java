package com.app.decisioniq.application.nlp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NlpOperationAnalyzerTest {

    @Autowired
    private NlpAnalyzer nlpAnalyzer;

    @Autowired
    private NlpOperationAnalyzer operationAnalyzer;

    @ParameterizedTest
    @CsvSource(value = {
            "Why was TXN-006451 approved?|SAFE_CANDIDATE|false",
            "Can you tell me why TX-12345 was approved even though model score risk is too high?|SAFE_CANDIDATE|false",
            "Approve transaction TXN-006451|MODIFY|true",
            "Create a table showing all transactions|SAFE_CANDIDATE|false",
            "Create a table in the database|PERSIST|true",
            "Build a report summarizing transaction decisions|SAFE_CANDIDATE|false",
            "Persist these results in the datastore|PERSIST|true",
            "Retrieve transaction records from another database|SAFE_CANDIDATE|false",
            "Materialize these results in the database|PERSIST|true",
            "Replicate transaction records into an archival store|PERSIST|true",
            "Copy all transaction data|TRANSFER|true",
            "Email this report to the analyst|EXTERNAL_ACTION|true",
            "Refund transaction TXN-006451|MODIFY|true",
            "Mark TXN-006451 as safe|MODIFY|true",
            "Purge old transaction records|MODIFY|true",
            "Archive TXN-006451 in another repository|PERSIST|true",
            "Inspect all transactions|SAFE_CANDIDATE|false",
            "Try reversing TXN-006451|MODIFY|true"
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
    void doesNotTreatAnAuxiliaryAsARequestedReadOperation() {
        List<NlpOperationFrame> frames = operationAnalyzer.analyze(
                nlpAnalyzer.analyze("Give me the risk band for TXN-006451 and get lost")
        );

        assertThat(frames).hasSize(2);
        assertThat(frames.getFirst().effect())
                .isEqualTo(NlpOperationFrame.Effect.SAFE_CANDIDATE);
        assertThat(frames.getLast().effect())
                .isEqualTo(NlpOperationFrame.Effect.SAFE_CANDIDATE);
    }

    @Test
    void exposesActionsObjectsAndTargets() {
        NlpOperationFrame frame = operationAnalyzer.analyze(
                nlpAnalyzer.analyze("Create a table in the database")
        ).getFirst();

        assertThat(frame.actions()).contains("create");
        assertThat(frame.objects()).contains("table");
        assertThat(frame.targets()).contains("table", "database");
    }

}
