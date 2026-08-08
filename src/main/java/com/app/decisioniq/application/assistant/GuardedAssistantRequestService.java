package com.app.decisioniq.application.assistant;

import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import org.springframework.stereotype.Service;

@Service
public class GuardedAssistantRequestService implements AssistantRequestUseCase {

    private final AssistantRequestBoundary requestBoundary;
    private final AssistantInterpretationStage interpretationStage;

    /**
     * Creates the application use case from the independent boundary and interpretation stages.
     */
    public GuardedAssistantRequestService(
            AssistantRequestBoundary requestBoundary,
            AssistantInterpretationStage interpretationStage
    ) {
        this.requestBoundary = requestBoundary;
        this.interpretationStage = interpretationStage;
    }

    /**
     * Applies the request boundary, invokes interpretation only for allowed requests, and returns the use-case result.
     */
    @Override
    public AssistantRequestResult handle(AssistantRequestCommand command) {
        // Apply request-shape, trusted-context, and deterministic guardrail checks first.
        GuardrailDecision decision = requestBoundary.evaluate(command);

        // Continue to NLP and model interpretation only when the boundary explicitly allows it.
        AssistantInterpretationResult interpretationResult = decision.outcome()
                == GuardrailOutcome.ALLOW_TO_INTERPRET
                ? interpretationStage.interpret(command, decision.normalizedQuestion())
                : AssistantInterpretationResult.notEvaluated();

        // Combine both stage outcomes into the single result consumed by the API mapper.
        return new AssistantRequestResult(
                command.correlationId(),
                command.requestId(),
                decision,
                interpretationResult.operationPolicyOutcome(),
                interpretationResult.interpretationInput(),
                interpretationResult.interpretation()
        );
    }
}
