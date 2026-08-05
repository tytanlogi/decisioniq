package com.app.decisioniq.application.guardrail.detector;

import com.app.decisioniq.config.guardrail.GuardrailProperties;

import java.util.Optional;

public interface GuardrailDetector {

    GuardrailProperties.DetectorId detectorId();

    Optional<GuardrailDetection> detect(String text);
}
