package com.app.decisioniq.application.assistant;

/**
 * Same-message user context attached to a lookup. These are untrusted assertions, not evidence.
 */
public record ResolvedLocalContext(
        String transactionId,
        String assertedOutcome,
        int sourceUnitIndex
) {
}
