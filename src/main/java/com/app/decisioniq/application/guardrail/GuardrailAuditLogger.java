package com.app.decisioniq.application.guardrail;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class GuardrailAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(GuardrailAuditLogger.class);

    private final GuardrailProperties properties;

    public GuardrailAuditLogger(GuardrailProperties properties) {
        this.properties = properties;
    }

    public String fingerprint(String question) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((question == null ? "" : question)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 8).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    public GuardrailDecision record(
            GuardrailDecision decision,
            String correlationId,
            int inputLength
    ) {
        log.info(
                "request_guardrail correlationId={} policyVersion={} outcome={} reasonCode={} fingerprint={} inputLength={}",
                correlationId,
                properties.version(),
                decision.outcome(),
                decision.reasonCode(),
                decision.auditFingerprint(),
                inputLength
        );
        return decision;
    }
}
