package com.app.decisioniq.application.nlp;

/**
 * Determines whether a clause establishes local context or asks DecisionIQ to perform a read.
 */
public enum NlpClauseRole {
    CONTEXT_STATEMENT,
    EXECUTABLE_REQUEST,
    IGNORED_HARMLESS_NOISE
}
