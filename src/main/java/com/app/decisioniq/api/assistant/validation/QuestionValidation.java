package com.app.decisioniq.api.assistant.validation;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class QuestionValidation {

    public static AssistantAskRequest normalizeQuestion(AssistantAskRequest assistantAskRequest) {
        if (assistantAskRequest.question() == null || assistantAskRequest.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        }
        return assistantAskRequest;
    }

}
