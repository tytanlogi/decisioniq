package com.app.decisioniq.domain.context;

import java.util.Objects;

/**
 * Identity and conversation context established at the trusted application boundary.
 * The source records whether identity came from authentication or a development adapter.
 */
public record TrustedRequestContext(
        String tenantId,
        String userId,
        String conversationId,
        String correlationId,
        RequestContextSource source
) {

    public TrustedRequestContext {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(conversationId, "conversationId is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(source, "source is required");
    }

    public boolean productionTrusted() {
        return source == RequestContextSource.AUTHENTICATED_CLAIMS;
    }
}
