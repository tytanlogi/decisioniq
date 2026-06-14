package com.app.decisioniq.assistant.intent.model;

import java.util.List;

public record ParsedQuestion(
        ParsedQuestionStatus status,
        String transactionId,
        List<ParsedAsk> asks,
        DecisionAssumption decisionAssumption,
        boolean clarificationRequired,
        String clarificationQuestion
) {
}
