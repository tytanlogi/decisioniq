package com.app.decisioniq.service.agent;

import com.app.decisioniq.agent.orchestration.DecisionAssistantOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DecisionService {

    private final DecisionAssistantOrchestrator decisionAssistantOrchestrator;

    @Autowired
    public DecisionService(DecisionAssistantOrchestrator decisionAssistantOrchestrator) {
        this.decisionAssistantOrchestrator = decisionAssistantOrchestrator;
    }

    public void askAgent(String question){
        decisionAssistantOrchestrator.initiateAgentOrchestration(question);
    }
}
