package com.app.decisioniq.agent.orchestration;

import com.app.decisioniq.agent.type.SampleAgent;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentOrchestration {

    private final CompiledGraph<SampleAgent> sampleAgentCompiledGraph;

    @Autowired
    public AgentOrchestration(CompiledGraph<SampleAgent> sampleAgentCompiledGraph) {
        this.sampleAgentCompiledGraph = sampleAgentCompiledGraph;
    }

    public String orchestrateSampleAgent(String question){
        SampleAgent sampleAgentState = sampleAgentCompiledGraph.invoke(Map.of(SampleAgent.QUESTION, question)).orElseThrow(() -> new IllegalStateException("Graph didn't return state"));
        return sampleAgentState.answer();

    }
}
