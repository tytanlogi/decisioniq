package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.guardrail.ConfiguredRequestBoundaryValidator;
import com.app.decisioniq.application.guardrail.RequestGuardrailService;
import com.app.decisioniq.domain.context.TrustedRequestContext;
import com.app.decisioniq.domain.context.TrustedRequestContextResolver;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GuardedRequestBoundaryService implements AssistantRequestBoundary {

    private static final Logger log = LoggerFactory.getLogger(GuardedRequestBoundaryService.class);

    private final ConfiguredRequestBoundaryValidator boundaryValidator;
    private final TrustedRequestContextResolver contextResolver;
    private final RequestGuardrailService guardrailService;

    /**
     * Creates the request boundary from shape validation, trusted-context resolution, and guardrail evaluation.
     */
    public GuardedRequestBoundaryService(
            ConfiguredRequestBoundaryValidator boundaryValidator,
            TrustedRequestContextResolver contextResolver,
            RequestGuardrailService guardrailService
    ) {
        this.boundaryValidator = boundaryValidator;
        this.contextResolver = contextResolver;
        this.guardrailService = guardrailService;
    }

    /**
     * Validates request fields, establishes request context, audits its trust source, and evaluates guardrails.
     */
    @Override
    public GuardrailDecision evaluate(AssistantRequestCommand command) {
        // Reject oversized or structurally invalid request fields before deeper processing.
        validateRequest(command);

        // Establish who and which tenant the application is allowed to process for this request.
        TrustedRequestContext context = resolveContext(command);

        // Record the context's trust source without exposing request or identity values.
        logContext(command, context);

        // Normalize the question and apply deterministic security detectors.
        return guardrailService.evaluate(command.question(), command.correlationId());
    }

    /**
     * Enforces configured size limits for all externally supplied request fields.
     */
    private void validateRequest(AssistantRequestCommand command) {
        boundaryValidator.validate(
                command.tenantId(),
                command.userId(),
                command.conversationId(),
                command.designation(),
                command.question()
        );
    }

    /**
     * Resolves the development or production trust context associated with the request.
     */
    private TrustedRequestContext resolveContext(AssistantRequestCommand command) {
        return contextResolver.resolve(
                command.tenantId(),
                command.userId(),
                command.conversationId(),
                command.correlationId()
        );
    }

    /**
     * Records context provenance without logging tenant, user, conversation, or question content.
     */
    private void logContext(AssistantRequestCommand command, TrustedRequestContext context) {
        log.info(
                "request_context correlationId={} requestId={} source={} productionTrusted={}",
                command.correlationId(),
                command.requestId(),
                context.source(),
                context.productionTrusted()
        );
    }
}
