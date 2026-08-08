package com.app.decisioniq.api.assistant.mapper;

import com.app.decisioniq.api.assistant.model.AssistantAnswerResponse;
import com.app.decisioniq.api.error.DecisionIqApiException;
import com.app.decisioniq.application.assistant.AssistantRequestResult;
import com.app.decisioniq.application.nlp.OperationPolicyOutcome;
import com.app.decisioniq.application.interpretation.InterpretationDisposition;
import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AssistantGuardrailHttpMapper {

    private final GuardrailProperties properties;

    public AssistantGuardrailHttpMapper(GuardrailProperties properties) {
        this.properties = properties;
    }

    public AssistantAnswerResponse toResponse(AssistantRequestResult result) {
        GuardrailDecision decision = result.guardrailDecision();
        if (decision.outcome() != GuardrailOutcome.ALLOW_TO_INTERPRET) {
            GuardrailProperties.ResponseTemplate template = decision.outcome()
                    == GuardrailOutcome.REQUEST_INVALID
                    ? properties.responses().requestInvalid()
                    : properties.responses().blockedUnsupportedContent();
            throw new DecisionIqApiException(
                    HttpStatus.BAD_REQUEST, template.code(), template.message(), result
            );
        }

        if (result.operationPolicyOutcome()
                == OperationPolicyOutcome.BLOCKED_UNSUPPORTED_OPERATION) {
            GuardrailProperties.ResponseTemplate template =
                    properties.responses().blockedUnsupportedContent();
            throw new DecisionIqApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    template.code(),
                    template.message(),
                    result
            );
        }

        if (result.interpretation() != null) {
            InterpretationDisposition disposition = result.interpretation().disposition();
            return new AssistantAnswerResponse(
                    answer(disposition, result),
                    responseCode(disposition),
                    result.correlationId(),
                    result.requestId(),
                    decision.outcome(),
                    decision.reasonCode(),
                    result.interpretation()
            );
        }

        GuardrailProperties.ResponseTemplate template =
                properties.responses().interpretationNotImplemented();
        throw new DecisionIqApiException(
                HttpStatus.NOT_IMPLEMENTED,
                template.code(),
                template.message(),
                result
        );
    }

    private String answer(
            InterpretationDisposition disposition,
            AssistantRequestResult result
    ) {
        return switch (disposition) {
            case READY_FOR_PLANNING ->
                    "The request was interpreted successfully. Evidence retrieval is the next phase.";
            case CLARIFICATION_REQUIRED -> result.interpretation()
                    .clarificationQuestions()
                    .stream()
                    .findFirst()
                    .orElse("Please clarify the requested transaction information.");
            case OUT_OF_SCOPE ->
                    "DecisionIQ supports read-only transaction decision intelligence questions.";
        };
    }

    private String responseCode(InterpretationDisposition disposition) {
        return disposition == InterpretationDisposition.READY_FOR_PLANNING
                ? "INTERPRETATION_READY"
                : disposition.name();
    }
}
