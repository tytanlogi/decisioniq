package com.app.decisioniq.application.assistant;

public record AssistantRequestCommand(
        String tenantId,
        String userId,
        String designation,
        String conversationId,
        String question,
        String correlationId,
        String requestId
) {
}
