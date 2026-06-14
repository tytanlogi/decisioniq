package com.app.decisioniq.assistant.graph.state;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

public class DecisionIQAgentState extends AgentState {

    public DecisionIQAgentState(Map<String, Object> initData) {
        super(initData);
    }

    public String question() {
        return this.<String>value(DecisionGraphStateKey.QUESTION)
                .orElseThrow(() -> new IllegalStateException("Question missing from graph state"));
    }

    public String parsedQuestionJson() {
        return this.<String>value(DecisionGraphStateKey.PARSED_QUESTION_JSON).orElse("");
    }

    public String queryResult() {
        return this.<String>value(DecisionGraphStateKey.QUERY_RESULT).orElse("");
    }

    public String answer() {
        return this.<String>value(DecisionGraphStateKey.ANSWER)
                .orElseThrow(() -> new IllegalStateException("Answer missing from graph state"));
    }
}
