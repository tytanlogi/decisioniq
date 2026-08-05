package com.app.decisioniq.domain.context;

public interface TrustedRequestContextResolver {

    TrustedRequestContext resolve(
            String tenantId,
            String userId,
            String conversationId,
            String correlationId
    );
}
