package com.app.decisioniq.application.guardrail;

import com.app.decisioniq.application.guardrail.detector.GuardrailDetection;
import com.app.decisioniq.application.guardrail.detector.GuardrailDetector;
import com.app.decisioniq.application.guardrail.detector.GuardrailDetectorRegistry;
import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GuardrailDetectionService {

    private final GuardrailProperties properties;
    private final GuardrailDetectorRegistry detectorRegistry;

    /**
     * Creates the detection engine from the enabled policy and available detector registry.
     */
    public GuardrailDetectionService(
            GuardrailProperties properties,
            GuardrailDetectorRegistry detectorRegistry
    ) {
        this.properties = properties;
        this.detectorRegistry = detectorRegistry;
    }

    /**
     * Runs enabled detectors in policy order and returns the highest-priority matching decision.
     */
    public Optional<GuardrailDetection> detect(String normalizedQuestion) {
        GuardrailDetection fallbackDetection = null;
        for (GuardrailProperties.DetectorId detectorId : properties.detectors().enabled()) {
            // Resolve only detectors explicitly enabled by the active versioned policy.
            GuardrailDetector detector = detectorRegistry.requiredDetector(detectorId);

            // Let the detector evaluate the same normalized question independently.
            Optional<GuardrailDetection> detection = detector.detect(normalizedQuestion);
            if (detection.isEmpty()) {
                continue;
            }

            // Return immediately when a whole-request security block is detected.
            if (detection.get().outcome() == GuardrailOutcome.BLOCKED_OBVIOUS_ATTACK_RAW_SQL) {
                return detection;
            }

            // Retain a lower-priority match in case no later detector produces a security block.
            fallbackDetection = detection.get();
        }
        return Optional.ofNullable(fallbackDetection);
    }
}
