package com.app.decisioniq.application.guardrail;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GuardrailAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(GuardrailAuditLogger.class);

    private final GuardrailProperties properties;

    /**
     * Creates an audit logger that includes the active policy version in every event.
     */
    public GuardrailAuditLogger(GuardrailProperties properties) {
        this.properties = properties;
    }

    /**
     * Writes a redacted guardrail audit event using only decision metadata and input length.
     */
    public void record(
            GuardrailDecision decision,
            String correlationId,
            int inputLength
    ) {
        log.info(
                "request_guardrail correlationId={} policyVersion={} outcome={} reasonCode={} inputLength={}",
                correlationId,
                properties.version(),
                decision.outcome(),
                decision.reasonCode(),
                inputLength
        );
    }
}
