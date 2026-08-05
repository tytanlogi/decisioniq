package com.app.decisioniq.infrastructure.context;

import com.app.decisioniq.domain.context.RequestContextSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DevelopmentTrustedRequestContextResolverTest {

    private final DevelopmentTrustedRequestContextResolver resolver =
            new DevelopmentTrustedRequestContextResolver();

    @Test
    void labelsClientIdentityAsDevelopmentOnlyAndNotProductionTrusted() {
        var context = resolver.resolve("tenant-a", "user-a", "conversation-a", "correlation-a");

        assertThat(context.tenantId()).isEqualTo("tenant-a");
        assertThat(context.userId()).isEqualTo("user-a");
        assertThat(context.source()).isEqualTo(RequestContextSource.DEVELOPMENT_REQUEST_FIELDS);
        assertThat(context.productionTrusted()).isFalse();
    }
}
