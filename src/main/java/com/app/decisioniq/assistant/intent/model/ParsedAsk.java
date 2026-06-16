package com.app.decisioniq.assistant.intent.model;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;

import java.io.Serializable;

public record ParsedAsk(
        DecisionIqIntent intent,
        String transactionId
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
