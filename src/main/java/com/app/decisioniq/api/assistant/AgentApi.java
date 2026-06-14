package com.app.decisioniq.api.assistant;

import com.app.decisioniq.assistant.orchestration.DecisionAssistantOrchestrator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentApi {

    private final DecisionAssistantOrchestrator decisionAssistantOrchestrator;

    public AgentApi(DecisionAssistantOrchestrator decisionAssistantOrchestrator) {
        this.decisionAssistantOrchestrator = decisionAssistantOrchestrator;
    }

    @PostMapping("/ask")
    public String askAgent(@RequestBody String question) {
        return decisionAssistantOrchestrator.answerQuestion(question);
    }
}
