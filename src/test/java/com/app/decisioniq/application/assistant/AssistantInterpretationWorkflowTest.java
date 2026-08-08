package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.catalog.CatalogInterpretationInputFactory;
import com.app.decisioniq.application.interpretation.RequestInterpretationService;
import com.app.decisioniq.application.nlp.NlpAnalysis;
import com.app.decisioniq.application.nlp.NlpAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.application.nlp.OperationPolicyOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantInterpretationWorkflowTest {

    @Mock
    private NlpAnalyzer nlpAnalyzer;

    @Mock
    private NlpOperationAnalyzer operationAnalyzer;

    @Mock
    private CatalogInterpretationInputFactory inputFactory;

    @Mock
    private RequestInterpretationService interpretationService;

    @Test
    void blocksBeforeCatalogAndLlmForUnsupportedOperation() {
        AssistantRequestCommand command = command("delete transaction TXN-006451");
        NlpAnalysis analysis = analysis(command.question());
        NlpOperationFrame frame = frame(command.question(), NlpOperationFrame.Effect.MODIFY);
        when(nlpAnalyzer.analyze(command.question())).thenReturn(analysis);
        when(operationAnalyzer.analyze(analysis)).thenReturn(List.of(frame));

        AssistantInterpretationWorkflow workflow = workflow();
        AssistantInterpretationResult result = workflow.interpret(command, command.question());

        assertThat(result.operationPolicyOutcome())
                .isEqualTo(OperationPolicyOutcome.BLOCKED_UNSUPPORTED_OPERATION);
        verifyNoInteractions(inputFactory, interpretationService);
    }

    @Test
    void buildsCatalogInputBeforeCallingInterpretationService() {
        AssistantRequestCommand command = command("show TXN-006451");
        NlpAnalysis analysis = analysis(command.question());
        NlpOperationFrame frame = frame(command.question(), NlpOperationFrame.Effect.SAFE_CANDIDATE);
        InterpretationInput input = new InterpretationInput("1.1", command.question(), List.of(), List.of());
        when(nlpAnalyzer.analyze(command.question())).thenReturn(analysis);
        when(operationAnalyzer.analyze(analysis)).thenReturn(List.of(frame));
        when(inputFactory.create(command.question(), List.of(frame), command.correlationId()))
                .thenReturn(input);
        when(interpretationService.interpret(input, command.correlationId(), command.requestId()))
                .thenReturn(Optional.empty());

        AssistantInterpretationResult result = workflow().interpret(command, command.question());

        assertThat(result.operationPolicyOutcome()).isEqualTo(OperationPolicyOutcome.ALLOWED);
        assertThat(result.interpretationInput()).isSameAs(input);
        assertThat(result.interpretation()).isNull();
    }

    private AssistantInterpretationWorkflow workflow() {
        return new AssistantInterpretationWorkflow(
                nlpAnalyzer,
                operationAnalyzer,
                inputFactory,
                interpretationService
        );
    }

    private AssistantRequestCommand command(String question) {
        return new AssistantRequestCommand(
                "tenant-a", "user-a", "Fraud Analyst", "conversation-a",
                question, "correlation-a", "request-a"
        );
    }

    private NlpAnalysis analysis(String text) {
        NlpAnalysis.Clause clause = new NlpAnalysis.Clause(0, text, 0, text.length(), 1, 1);
        NlpAnalysis.Sentence sentence = new NlpAnalysis.Sentence(
                0, text, 0, text.length(), List.of(), List.of(), List.of(clause)
        );
        return new NlpAnalysis(text, List.of(sentence));
    }

    private NlpOperationFrame frame(String text, NlpOperationFrame.Effect effect) {
        return new NlpOperationFrame(0, 0, text, List.of(), List.of(), List.of(), effect);
    }
}
