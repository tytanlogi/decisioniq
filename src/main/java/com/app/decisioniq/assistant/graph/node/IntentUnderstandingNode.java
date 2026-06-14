package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.service.intent.IntentUnderstandingService;
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
    public Map<String, Object> apply(DecisionIQAgentState state) {
        String parsedQuestionJson = intentUnderstandingService.parseIntent(state.question());
        log.info("Parsed question response received {}", parsedQuestionJson);
        return Map.of(DecisionGraphStateKey.PARSED_QUESTION_JSON, parsedQuestionJson);
    }
}
