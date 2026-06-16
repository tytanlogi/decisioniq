package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.service.intent.IntentUnderstandingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class IntentUnderstandingNode implements NodeAction<DecisionIQAgentState> {

    private final IntentUnderstandingService intentUnderstandingService;

    public IntentUnderstandingNode(IntentUnderstandingService intentUnderstandingService) {
        this.intentUnderstandingService = intentUnderstandingService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) throws JsonProcessingException {
        ParsedQuestion parsedQuestionJson = intentUnderstandingService.parseIntent(state.question());
        return Map.of(DecisionGraphStateKey.INTENT_UNDERSTANDING_KEY, parsedQuestionJson);
    }

}
