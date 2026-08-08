package com.app.decisioniq.application.guardrail;

import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.springframework.stereotype.Service;

@Service
public class RequestGuardrailService {

    private final GuardrailTextNormalizer normalizer;
    private final GuardrailInputValidator inputValidator;
    private final GuardrailDetectionService detectionService;
    private final GuardrailAuditLogger auditLogger;

    /**
     * Creates the guardrail coordinator from focused validation, detection, audit, and normalization services.
     */
    public RequestGuardrailService(
            GuardrailTextNormalizer normalizer,
            GuardrailInputValidator inputValidator,
            GuardrailDetectionService detectionService,
            GuardrailAuditLogger auditLogger
    ) {
        this.normalizer = normalizer;
        this.inputValidator = inputValidator;
        this.detectionService = detectionService;
        this.auditLogger = auditLogger;
    }

    /**
     * Normalizes and validates a question, applies all configured security detectors,
     * and records the resulting guardrail decision without logging the raw question.
     */
    public GuardrailDecision evaluate(String question, String correlationId) {
        // Remove harmless formatting variance without changing identifiers or semantic values.
        String normalized = normalizer.normalizeQuestion(question);

        // Retain only input size as additional safe audit context.
        int inputLength = question == null ? 0 : question.length();

        // Validate the normalized input and apply configured security detectors.
        GuardrailDecision decision = createDecision(question, normalized);

        // Persist the final policy outcome after all guardrail decisions are complete.
        auditLogger.record(decision, correlationId, inputLength);
        return decision;
    }

    /**
     * Creates an invalid, blocked, or allowed decision from the validated guardrail inputs.
     */
    private GuardrailDecision createDecision(
            String originalQuestion,
            String normalizedQuestion
    ) {
        // Return the standard invalid outcome before running content detectors.
        if (!inputValidator.isValid(originalQuestion, normalizedQuestion)) {
            return invalidDecision(normalizedQuestion);
        }

        // Convert the highest-priority detector match, or absence of a match, into a domain decision.
        return detectionService.detect(normalizedQuestion)
                .map(detection -> new GuardrailDecision(
                        detection.outcome(), normalizedQuestion, detection.reasonCode()
                ))
                .orElseGet(() -> new GuardrailDecision(
                        GuardrailOutcome.ALLOW_TO_INTERPRET,
                        normalizedQuestion,
                        GuardrailReasonCode.READY_FOR_INTERPRETATION
                ));
    }

    /**
     * Creates the standard malformed-input decision.
     */
    private GuardrailDecision invalidDecision(String normalized) {
        return new GuardrailDecision(
                GuardrailOutcome.REQUEST_INVALID,
                normalized,
                GuardrailReasonCode.MALFORMED_INPUT
        );
    }
}
