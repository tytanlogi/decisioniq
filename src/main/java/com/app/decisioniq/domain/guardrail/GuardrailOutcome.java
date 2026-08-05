package com.app.decisioniq.domain.guardrail;

public enum GuardrailOutcome {
    ALLOW_TO_INTERPRET,
    BLOCKED_UNSUPPORTED_MUTATION,
    BLOCKED_OBVIOUS_ATTACK_RAW_SQL,
    REQUEST_INVALID
}
