package com.app.decisioniq.application.interpretation;

import com.app.decisioniq.application.assistant.InterpretationInput;

import java.util.Optional;

public interface RequestInterpretationService {

    Optional<QueryInterpretation> interpret(
            InterpretationInput input,
            String correlationId,
            String requestId
    );
}
