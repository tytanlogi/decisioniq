package com.app.decisioniq.application.guardrail;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConfiguredRequestBoundaryValidator {

    private final GuardrailProperties properties;

    public ConfiguredRequestBoundaryValidator(GuardrailProperties properties) {
        this.properties = properties;
    }

    public void validate(
            String tenantId,
            String userId,
            String conversationId,
            String designation,
            String question
    ) {
        List<RequestBoundaryViolation> violations = new ArrayList<>();
        validateLength("tenantId", tenantId,
                properties.limits().maxTenantIdCharacters(), violations);
        validateLength("userId", userId,
                properties.limits().maxUserIdCharacters(), violations);
        validateLength("conversationId", conversationId,
                properties.limits().maxConversationIdCharacters(), violations);
        validateLength("designation", designation,
                properties.limits().maxDesignationCharacters(), violations);
        validateLength("question", question,
                properties.limits().maxQuestionCharacters(), violations);

        if (!violations.isEmpty()) {
            throw new RequestBoundaryValidationException(violations);
        }
    }

    private void validateLength(
            String field,
            String value,
            int maximum,
            List<RequestBoundaryViolation> violations
    ) {
        if (value != null && value.length() > maximum) {
            violations.add(new RequestBoundaryViolation(
                    field,
                    field + " must not exceed " + maximum + " characters"
            ));
        }
    }
}
