package com.app.decisioniq.agent.orchestration;

import com.app.decisioniq.agent.type.DecisionIQAgent;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DecisionAssistantOrchestrator {

    private final CompiledGraph<DecisionIQAgent> decisionIQAgentCompiledGraph;

    @Autowired
    public DecisionAssistantOrchestrator(CompiledGraph<DecisionIQAgent> decisionIQAgentCompiledGraph) {
        this.decisionIQAgentCompiledGraph = decisionIQAgentCompiledGraph;
    }

    public String initiateAgentOrchestration(String question) {
        DecisionIQAgent decisionIQAgent = decisionIQAgentCompiledGraph.invoke(Map.of(DecisionIQAgent.INTENT, question)).orElseThrow(() -> new IllegalStateException("Graph didn't return state"));
        return decisionIQAgent.answer();
    }
}
