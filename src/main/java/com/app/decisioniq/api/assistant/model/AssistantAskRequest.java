package com.app.decisioniq.api.assistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload accepted by the assistant UI.
 * Tenant and user values are development inputs until a production identity resolver
 * replaces them with authenticated claims.
 */
public record AssistantAskRequest(
        @NotBlank(message = "Tenant id is required")
        @Size(max = 256, message = "Tenant id exceeds the transport limit")
        String tenantId,

        @NotBlank(message = "User id is required")
        @Size(max = 256, message = "User id exceeds the transport limit")
        String userId,

        @Size(max = 200, message = "Designation exceeds the transport limit")
        String designation,

        @NotBlank(message = "Conversation id is required")
        @Size(max = 256, message = "Conversation id exceeds the transport limit")
        String conversationId,

        @NotBlank(message = "Question is required")
        @Size(max = 8192, message = "Question exceeds the transport limit")
        String question
) { }
