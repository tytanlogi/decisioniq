package com.app.decisioniq.application.assistant;

public interface AssistantRequestUseCase {

    AssistantRequestResult handle(AssistantRequestCommand command);
}
