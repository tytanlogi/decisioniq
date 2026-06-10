package com.app.decisioniq.service;

import com.app.decisioniq.agent.orchestration.AgentOrchestration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final AgentOrchestration agentOrchestration;

    @Autowired
    public AgentService(AgentOrchestration agentOrchestration) {
        this.agentOrchestration = agentOrchestration;
    }

    public String processQuestion(String question) {
       return agentOrchestration.orchestrateSampleAgent(question);
    }
}
