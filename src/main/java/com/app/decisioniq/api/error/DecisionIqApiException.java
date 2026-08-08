package com.app.decisioniq.api.error;

import com.app.decisioniq.application.assistant.AssistantRequestResult;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import org.springframework.http.HttpStatus;

public class DecisionIqApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final GuardrailDecision guardrailDecision;
    private final AssistantRequestResult requestResult;

    public DecisionIqApiException(
            HttpStatus status,
            String code,
            String message,
            GuardrailDecision guardrailDecision
    ) {
        this(status, code, message, guardrailDecision, null);
    }

    public DecisionIqApiException(
            HttpStatus status,
            String code,
            String message,
            AssistantRequestResult requestResult
    ) {
        this(
                status,
                code,
                message,
                requestResult.guardrailDecision(),
                requestResult
        );
    }

    private DecisionIqApiException(
            HttpStatus status,
            String code,
            String message,
            GuardrailDecision guardrailDecision,
            AssistantRequestResult requestResult
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.guardrailDecision = guardrailDecision;
        this.requestResult = requestResult;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public GuardrailDecision guardrailDecision() {
        return guardrailDecision;
    }

    public AssistantRequestResult requestResult() {
        return requestResult;
    }
}
