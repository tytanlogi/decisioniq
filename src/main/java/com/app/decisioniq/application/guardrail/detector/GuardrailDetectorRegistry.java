package com.app.decisioniq.application.guardrail.detector;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class GuardrailDetectorRegistry {

    private final Map<GuardrailProperties.DetectorId, GuardrailDetector> detectors;

    /**
     * Indexes detector implementations once and rejects duplicate detector identifiers at startup.
     */
    public GuardrailDetectorRegistry(List<GuardrailDetector> detectorList) {
        this.detectors = indexDetectors(detectorList);
    }

    /**
     * Returns the implementation for an enabled detector or fails fast on invalid configuration.
     */
    public GuardrailDetector requiredDetector(GuardrailProperties.DetectorId detectorId) {
        GuardrailDetector detector = detectors.get(detectorId);
        if (detector == null) {
            throw new IllegalStateException("Configured guardrail detector is unavailable: " + detectorId);
        }
        return detector;
    }

    /**
     * Builds an immutable detector index and prevents ambiguous duplicate implementations.
     */
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
}
