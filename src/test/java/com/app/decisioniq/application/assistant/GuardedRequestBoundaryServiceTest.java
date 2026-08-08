package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.guardrail.ConfiguredRequestBoundaryValidator;
import com.app.decisioniq.application.guardrail.RequestGuardrailService;
import com.app.decisioniq.domain.context.RequestContextSource;
import com.app.decisioniq.domain.context.TrustedRequestContext;
import com.app.decisioniq.domain.context.TrustedRequestContextResolver;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardedRequestBoundaryServiceTest {

    @Mock
    private ConfiguredRequestBoundaryValidator boundaryValidator;

    @Mock
    private TrustedRequestContextResolver contextResolver;

    @Mock
    private RequestGuardrailService guardrailService;

    @Test
    void validatesContextBeforeEvaluatingGuardrail() {
        AssistantRequestCommand command = new AssistantRequestCommand(
                "tenant-a", "user-a", "Fraud Analyst", "conversation-a",
                "show TXN-006451", "correlation-a", "request-a"
        );
        TrustedRequestContext context = new TrustedRequestContext(
                "tenant-a", "user-a", "conversation-a", "correlation-a",
                RequestContextSource.DEVELOPMENT_REQUEST_FIELDS
        );
        GuardrailDecision decision = new GuardrailDecision(
                GuardrailOutcome.ALLOW_TO_INTERPRET,
                command.question(),
                GuardrailReasonCode.READY_FOR_INTERPRETATION
        );
        when(contextResolver.resolve(
                command.tenantId(), command.userId(), command.conversationId(), command.correlationId()
        )).thenReturn(context);
        when(guardrailService.evaluate(command.question(), command.correlationId()))
                .thenReturn(decision);

        GuardedRequestBoundaryService service = new GuardedRequestBoundaryService(
                boundaryValidator,
                contextResolver,
                guardrailService
        );
        GuardrailDecision result = service.evaluate(command);

        assertThat(result).isSameAs(decision);
        InOrder order = inOrder(boundaryValidator, contextResolver, guardrailService);
        order.verify(boundaryValidator).validate(
                command.tenantId(), command.userId(), command.conversationId(),
                command.designation(), command.question()
        );
        order.verify(contextResolver).resolve(
                command.tenantId(), command.userId(), command.conversationId(), command.correlationId()
        );
        order.verify(guardrailService).evaluate(command.question(), command.correlationId());
    }
}
