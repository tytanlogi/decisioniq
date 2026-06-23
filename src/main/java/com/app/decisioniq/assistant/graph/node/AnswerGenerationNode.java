package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.service.answer.AnswerGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey.ANSWER_KEY;

@Component
@Slf4j
public class AnswerGenerationNode implements NodeAction<DecisionIQAgentState> {

    private final AnswerGenerationService answerGenerationService;

    public AnswerGenerationNode(AnswerGenerationService answerGenerationService) {
        this.answerGenerationService = answerGenerationService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        log.info("Generating answer for the query {}",state.getQuery());
        return Map.of(ANSWER_KEY,"Hello Answered");
    }
}
