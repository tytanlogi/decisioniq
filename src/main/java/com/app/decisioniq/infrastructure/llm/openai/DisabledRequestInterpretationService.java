package com.app.decisioniq.infrastructure.llm.openai;

import com.app.decisioniq.application.assistant.InterpretationInput;
import com.app.decisioniq.application.interpretation.QueryInterpretation;
import com.app.decisioniq.application.interpretation.RequestInterpretationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnProperty(
        prefix = "decisioniq.llm.openai",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DisabledRequestInterpretationService implements RequestInterpretationService {

    @Override
    public Optional<QueryInterpretation> interpret(
            InterpretationInput input,
            String correlationId,
            String requestId
    ) {
        return Optional.empty();
    }
}
