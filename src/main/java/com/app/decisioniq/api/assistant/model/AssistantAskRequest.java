package com.app.decisioniq.api.assistant.model;

/**
 * Request payload accepted by the assistant UI.
 * tenantId and userRole are part of the public API contract so the graph can enforce
 * tenant-scoped retrieval and role-aware answers as those stages are connected.
 */
public record AssistantAskRequest(
        String tenantId,
        String userRole,
        String question
) {
}
