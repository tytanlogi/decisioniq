package com.app.decisioniq.application.nlp;

import java.util.Objects;

/**
 * A user-supplied, same-message reference. Its asserted outcome is never treated as trusted data.
 */
public record NlpContextCandidate(String transactionId, String assertedOutcome) {
    public NlpContextCandidate {
        Objects.requireNonNull(transactionId, "transactionId is required");
        if (transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
    }
}
