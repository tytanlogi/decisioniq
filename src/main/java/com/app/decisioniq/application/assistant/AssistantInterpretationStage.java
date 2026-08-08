package com.app.decisioniq.application.assistant;

public interface AssistantInterpretationStage {

    /**
     * Interprets a normalized question that has already passed the request boundary.
     */
    AssistantInterpretationResult interpret(
            AssistantRequestCommand command,
            String normalizedQuestion
    );
}
