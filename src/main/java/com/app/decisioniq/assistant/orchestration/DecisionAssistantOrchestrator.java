package com.app.decisioniq.assistant.orchestration;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DecisionAssistantOrchestrator {

    private final CompiledGraph<DecisionIQAgentState> decisionAssistantGraph;

    public DecisionAssistantOrchestrator(CompiledGraph<DecisionIQAgentState> decisionAssistantGraph) {
        this.decisionAssistantGraph = decisionAssistantGraph;
    }

    public String answerQuestion(AssistantAskRequest assistantAskRequest) {
        DecisionIQAgentState finalState = decisionAssistantGraph.invoke(Map.of(
                DecisionGraphStateKey.QUESTION_KEY, assistantAskRequest.question(),
                DecisionGraphStateKey.TENANT_ID_KEY,assistantAskRequest.tenantId()
        )).orElseThrow(() -> new IllegalStateException("Graph did not return a final state"));

        return finalState.answer();
    }
}
