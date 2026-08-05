package com.app.decisioniq.application.guardrail;

import com.app.decisioniq.application.guardrail.detector.GuardrailDetection;
import com.app.decisioniq.application.guardrail.detector.GuardrailDetector;
import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RequestGuardrailService {

    private final GuardrailTextNormalizer normalizer;
    private final GuardrailInputValidator inputValidator;
    private final GuardrailAuditLogger auditLogger;
    private final GuardrailProperties properties;
    private final Map<GuardrailProperties.DetectorId, GuardrailDetector> detectors;

    public RequestGuardrailService(
            GuardrailTextNormalizer normalizer,
            GuardrailInputValidator inputValidator,
            GuardrailAuditLogger auditLogger,
            GuardrailProperties properties,
            List<GuardrailDetector> detectorList
    ) {
        this.normalizer = normalizer;
        this.inputValidator = inputValidator;
        this.auditLogger = auditLogger;
        this.properties = properties;
        this.detectors = indexDetectors(detectorList);
    }

    public GuardrailDecision evaluate(String question, String correlationId) {
        String normalized = normalizer.normalizeQuestion(question);
        String fingerprint = auditLogger.fingerprint(question);
        int inputLength = question == null ? 0 : question.length();

        if (inputValidator.isInvalid(question, normalized)) {
            return auditLogger.record(
                    invalidDecision(normalized, fingerprint),
                    correlationId,
                    inputLength
            );
        }

        GuardrailDecision decision = detectUnsafeContent(normalized)
                .map(detection -> new GuardrailDecision(
                        detection.outcome(), normalized, detection.reasonCode(), fingerprint
                ))
                .orElseGet(() -> new GuardrailDecision(
                        GuardrailOutcome.ALLOW_TO_INTERPRET,
                        normalized,
                        GuardrailReasonCode.READY_FOR_INTERPRETATION,
                        fingerprint
                ));
        return auditLogger.record(decision, correlationId, inputLength);
    }

    private Optional<GuardrailDetection> detectUnsafeContent(String normalizedQuestion) {
        GuardrailDetection mutation = null;
        for (GuardrailProperties.DetectorId detectorId : properties.detectors().enabled()) {
            GuardrailDetector detector = detectors.get(detectorId);
            if (detector == null) {
                throw new IllegalStateException("Configured guardrail detector is unavailable: " + detectorId);
            }
            Optional<GuardrailDetection> detection = detector.detect(normalizedQuestion);
            if (detection.isEmpty()) {
                continue;
            }
            if (detection.get().outcome() == GuardrailOutcome.BLOCKED_OBVIOUS_ATTACK_RAW_SQL) {
                return detection;
            }
            mutation = detection.get();
        }
        return Optional.ofNullable(mutation);
    }

    private Map<GuardrailProperties.DetectorId, GuardrailDetector> indexDetectors(
            List<GuardrailDetector> detectorList
    ) {
        Map<GuardrailProperties.DetectorId, GuardrailDetector> indexed =
                new EnumMap<>(GuardrailProperties.DetectorId.class);
        detectorList.forEach(detector -> {
            GuardrailDetector previous = indexed.put(detector.detectorId(), detector);
            if (previous != null) {
                throw new IllegalStateException("Duplicate guardrail detector: " + detector.detectorId());
            }
        });
        return Map.copyOf(indexed);
    }

    private GuardrailDecision invalidDecision(String normalized, String fingerprint) {
        return new GuardrailDecision(
                GuardrailOutcome.REQUEST_INVALID,
                normalized,
                GuardrailReasonCode.MALFORMED_INPUT,
                fingerprint
        );
    }
}
