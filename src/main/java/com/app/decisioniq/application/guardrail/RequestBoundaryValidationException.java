package com.app.decisioniq.application.guardrail;

import java.util.List;

public class RequestBoundaryValidationException extends RuntimeException {

    private final List<RequestBoundaryViolation> violations;

    public RequestBoundaryValidationException(List<RequestBoundaryViolation> violations) {
        super("Request validation failed");
        this.violations = List.copyOf(violations);
    }

    public List<RequestBoundaryViolation> violations() {
        return violations;
    }
}
