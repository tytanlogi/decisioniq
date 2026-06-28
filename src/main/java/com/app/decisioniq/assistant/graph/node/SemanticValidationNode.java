package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.service.semantic.SemanticService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SemanticValidationNode implements NodeAction<DecisionIQAgentState> {

    private final SemanticService semanticService;

    @Autowired
    public SemanticValidationNode(SemanticService semanticService) {
        this.semanticService = semanticService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state){
        boolean isValid = semanticService.compareEmbeddings(state.question());
        return Map.of(DecisionGraphStateKey.SEMANTIC_VALIDATION_KEY,isValid);
    }
}
