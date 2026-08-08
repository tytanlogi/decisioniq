package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.nlp.OperationPolicyOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardedAssistantRequestServiceTest {

    @Mock
    private AssistantRequestBoundary requestBoundary;

    @Mock
    private AssistantInterpretationStage interpretationStage;

    @Test
    void delegatesAllowedRequestToInterpretationWorkflow() {
        AssistantRequestCommand command = command();
        GuardrailDecision decision = allowedDecision();
        InterpretationInput input = new InterpretationInput("1.1", command.question(), List.of(), List.of());
        AssistantInterpretationResult interpretationResult =
                AssistantInterpretationResult.allowed(input, null);
        when(requestBoundary.evaluate(command)).thenReturn(decision);
        when(interpretationStage.interpret(command, decision.normalizedQuestion()))
                .thenReturn(interpretationResult);

        GuardedAssistantRequestService service = new GuardedAssistantRequestService(
                requestBoundary,
                interpretationStage
        );
        AssistantRequestResult result = service.handle(command);

        assertThat(result.guardrailDecision()).isSameAs(decision);
        assertThat(result.operationPolicyOutcome()).isEqualTo(OperationPolicyOutcome.ALLOWED);
        assertThat(result.interpretationInput()).isSameAs(input);
    }

    @Test
    void stopsBeforeInterpretationWhenGuardrailBlocksRequest() {
        AssistantRequestCommand command = command();
        GuardrailDecision blocked = new GuardrailDecision(
                GuardrailOutcome.BLOCKED_OBVIOUS_ATTACK_RAW_SQL,
                command.question(),
                GuardrailReasonCode.OBVIOUS_RAW_SQL
        );
        when(requestBoundary.evaluate(command)).thenReturn(blocked);

        GuardedAssistantRequestService service = new GuardedAssistantRequestService(
                requestBoundary,
                interpretationStage
        );
        AssistantRequestResult result = service.handle(command);

        assertThat(result.operationPolicyOutcome()).isEqualTo(OperationPolicyOutcome.NOT_EVALUATED);
        assertThat(result.interpretationInput()).isNull();
        verifyNoInteractions(interpretationStage);
    }

    private AssistantRequestCommand command() {
        return new AssistantRequestCommand(
                "tenant-a",
                "user-a",
                "Fraud Analyst",
                "conversation-a",
                "show TXN-006451",
                "correlation-a",
                "request-a"
        );
    }

    private GuardrailDecision allowedDecision() {
        return new GuardrailDecision(
                GuardrailOutcome.ALLOW_TO_INTERPRET,
                "show TXN-006451",
                GuardrailReasonCode.READY_FOR_INTERPRETATION
        );
    }
}
