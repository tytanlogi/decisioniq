package com.app.decisioniq.application.guardrail;

import java.util.List;

public class RequestBoundaryValidationException extends RuntimeException {

    private final List<RequestBoundaryViolation> violations;

    /**
     * Creates an immutable validation exception containing all request-boundary violations.
     */
    public RequestBoundaryValidationException(List<RequestBoundaryViolation> violations) {
        super("Request validation failed");
        this.violations = List.copyOf(violations);
    }

    /**
     * Returns the immutable field-level violations used by the API error mapper.
     */
    public List<RequestBoundaryViolation> violations() {
        return violations;
    }
}
