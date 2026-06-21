package com.app.decisioniq.api.assistant;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import com.app.decisioniq.api.assistant.model.AssistantAnswerResponse;
import com.app.decisioniq.assistant.orchestration.DecisionAssistantOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping({"/agent", "/api/assistant"})
public class AgentApi {

    private final DecisionAssistantOrchestrator decisionAssistantOrchestrator;

    public AgentApi(DecisionAssistantOrchestrator decisionAssistantOrchestrator) {
        this.decisionAssistantOrchestrator = decisionAssistantOrchestrator;
    }

    @PostMapping(
            value = "/ask",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AssistantAnswerResponse askAgent(@RequestBody AssistantAskRequest request) {
        return answerQuestion(request.question());
    }

    @PostMapping(
            value = "/ask",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AssistantAnswerResponse askLegacyAgent(@RequestBody String question) {
        return answerQuestion(question);
    }

    private AssistantAnswerResponse answerQuestion(String question) {
        String normalizedQuestion = normalizeQuestion(question);
        String answer = decisionAssistantOrchestrator.answerQuestion(normalizedQuestion);
        return new AssistantAnswerResponse(answer);
    }

    private static String normalizeQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        }
        return question.trim();
    }
}
