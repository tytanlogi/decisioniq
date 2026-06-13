package com.app.decisioniq.api;

import com.app.decisioniq.agent.orchestration.DecisionAssistantOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentApi {

    private final DecisionAssistantOrchestrator decisionAssistantOrchestrator;

    @Autowired
    public AgentApi(DecisionAssistantOrchestrator decisionAssistantOrchestrator) {
        this.decisionAssistantOrchestrator = decisionAssistantOrchestrator;
    }

    @PostMapping("/ask")
    public String askAgent(@RequestBody  String question){
        return decisionAssistantOrchestrator.initiateAgentOrchestration(question);
    }
}
