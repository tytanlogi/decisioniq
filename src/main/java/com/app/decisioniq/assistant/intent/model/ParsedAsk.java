package com.app.decisioniq.assistant.intent.model;

public record ParsedAsk(
        DecisionIqIntent intent,
        String transactionId
) {
}
