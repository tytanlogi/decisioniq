package com.app.decisioniq.domain.guardrail;

public enum GuardrailReasonCode {
    READY_FOR_INTERPRETATION,
    OBVIOUS_RAW_SQL,
    OBVIOUS_SCRIPT_ATTACK,
    OBVIOUS_INSTRUCTION_BYPASS,
    MALFORMED_INPUT
}
