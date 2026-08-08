package com.app.decisioniq.application.guardrail.detector;

import com.app.decisioniq.config.guardrail.GuardrailProperties;

import java.util.Optional;

public interface GuardrailDetector {

    /**
     * Identifies the detector so configuration can enable it without coupling to its class.
     */
    GuardrailProperties.DetectorId detectorId();

    /**
     * Evaluates normalized text and returns a decision only when this detector finds a match.
     */
    Optional<GuardrailDetection> detect(String text);
}
