package com.app.decisioniq.api.filter;

import com.app.decisioniq.config.guardrail.GuardrailPatternRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdentityFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String REQUEST_HEADER = "X-Request-Id";
    public static final String CORRELATION_ATTRIBUTE = "decisioniq.correlationId";
    public static final String REQUEST_ATTRIBUTE = "decisioniq.requestId";

    private final GuardrailPatternRegistry patternRegistry;

    public RequestIdentityFilter(GuardrailPatternRegistry patternRegistry) {
        this.patternRegistry = patternRegistry;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(CORRELATION_HEADER));
        String requestId = UUID.randomUUID().toString();

        request.setAttribute(CORRELATION_ATTRIBUTE, correlationId);
        request.setAttribute(REQUEST_ATTRIBUTE, requestId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader(REQUEST_HEADER, requestId);
        MDC.put("correlationId", correlationId);
        MDC.put("requestId", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("correlationId");
        }
    }

    private String resolveCorrelationId(String suppliedValue) {
        if (suppliedValue != null && patternRegistry.correlationId().matcher(suppliedValue).matches()) {
            return suppliedValue;
        }
        return UUID.randomUUID().toString();
    }
}
