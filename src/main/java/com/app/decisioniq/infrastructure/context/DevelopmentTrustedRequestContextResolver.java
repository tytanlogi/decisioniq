package com.app.decisioniq.infrastructure.context;

import com.app.decisioniq.domain.context.RequestContextSource;
import com.app.decisioniq.domain.context.TrustedRequestContext;
import com.app.decisioniq.domain.context.TrustedRequestContextResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Development-only adapter that accepts tenant and user identifiers from the request.
 * These values are not authenticated and must never be treated as a production trust boundary.
 * A production profile must provide a resolver backed by verified identity claims.
 */
@Component
@Profile("!prod")
public class DevelopmentTrustedRequestContextResolver implements TrustedRequestContextResolver {

    @Override
    public TrustedRequestContext resolve(
            String tenantId,
            String userId,
            String conversationId,
            String correlationId
    ) {
        return new TrustedRequestContext(
                tenantId,
                userId,
                conversationId,
                correlationId,
                RequestContextSource.DEVELOPMENT_REQUEST_FIELDS
        );
    }
}
