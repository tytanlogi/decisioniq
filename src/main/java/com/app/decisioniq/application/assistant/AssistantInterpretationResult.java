package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.interpretation.QueryInterpretation;
import com.app.decisioniq.application.nlp.OperationPolicyOutcome;

public record AssistantInterpretationResult(
        OperationPolicyOutcome operationPolicyOutcome,
        InterpretationInput interpretationInput,
        QueryInterpretation interpretation
) {

    /**
     * Represents a request that did not reach interpretation because the earlier boundary rejected it.
     */
    public static AssistantInterpretationResult notEvaluated() {
        return new AssistantInterpretationResult(
                OperationPolicyOutcome.NOT_EVALUATED,
                null,
                null
        );
    }

    /**
     * Represents a request blocked by the deterministic NLP operation policy.
     */
    public static AssistantInterpretationResult blocked() {
        return new AssistantInterpretationResult(
                OperationPolicyOutcome.BLOCKED_UNSUPPORTED_OPERATION,
                null,
                null
        );
    }

    /**
     * Represents an allowed request together with its governed model input and optional interpretation.
     */
    public static AssistantInterpretationResult allowed(
            InterpretationInput input,
            QueryInterpretation interpretation
    ) {
        return new AssistantInterpretationResult(
                OperationPolicyOutcome.ALLOWED,
                input,
                interpretation
        );
    }
}
