package com.app.decisioniq.application.assistant;

/**
 * The terminal result of deterministic processing immediately before catalog retrieval.
 */
public enum PreCatalogDisposition {
    READY_FOR_CATALOG,
    IGNORED_HARMLESS_NOISE,
    CLARIFY_REQUIRED,
    BLOCKED_BY_GUARDRAIL,
    BLOCKED_BY_OPERATION_POLICY
}
