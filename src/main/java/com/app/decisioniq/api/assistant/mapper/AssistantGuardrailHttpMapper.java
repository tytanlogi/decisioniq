package com.app.decisioniq.api.assistant.mapper;

import com.app.decisioniq.api.assistant.model.AssistantAnswerResponse;
import com.app.decisioniq.api.error.DecisionIqApiException;
import com.app.decisioniq.application.assistant.AssistantRequestResult;
import com.app.decisioniq.application.nlp.OperationPolicyOutcome;
import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.config.catalog.CatalogRelevanceProperties;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision;
import com.app.decisioniq.domain.catalog.CatalogRelevanceOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AssistantGuardrailHttpMapper {

    private final GuardrailProperties properties;
    private final CatalogRelevanceProperties relevanceProperties;

    public AssistantGuardrailHttpMapper(
            GuardrailProperties properties,
            CatalogRelevanceProperties relevanceProperties
    ) {
        this.properties = properties;
        this.relevanceProperties = relevanceProperties;
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

        CatalogRelevanceDecision relevance = result.catalogRelevanceDecision();
        if (relevance == null) {
            throw new IllegalStateException("Catalog relevance decision is required after guardrail approval");
        }
        if (relevance.outcome() == CatalogRelevanceOutcome.SUPPORTED) {
            GuardrailProperties.ResponseTemplate template =
                    properties.responses().interpretationNotImplemented();
            throw new DecisionIqApiException(
                    HttpStatus.NOT_IMPLEMENTED,
                    template.code(),
                    template.message(),
                    result
            );
        }

        CatalogRelevanceProperties.ResponseTemplate template = switch (relevance.outcome()) {
            case AMBIGUOUS -> relevanceProperties.responses().ambiguous();
            case OUT_OF_SCOPE -> relevanceProperties.responses().outOfScope();
            case INSUFFICIENT_CONTEXT -> relevanceProperties.responses().insufficientContext();
            case SUPPORTED -> throw new IllegalStateException("Supported relevance was already handled");
        };
        throw new DecisionIqApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                template.code(),
                template.message(),
                result
        );
    }
}
