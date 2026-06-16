package com.app.decisioniq.assistant.graph.state;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.Optional;

public class DecisionIQAgentState extends AgentState {

    public DecisionIQAgentState(Map<String, Object> initData) {
        super(initData);
    }

    public String question() {
        return this.<String>value(DecisionGraphStateKey.QUESTION_KEY)
                .orElseThrow(() -> new IllegalStateException("Question missing from graph state"));
    }

    public Optional<ParsedQuestion> intent() {
        return this.<ParsedQuestion>value(DecisionGraphStateKey.INTENT_UNDERSTANDING_KEY);
    }

    public Optional<ParsedQuestion> clarifyIntent(){
        return this.value(DecisionGraphStateKey.INTENT_UNDERSTANDING_KEY);
    }

    public String query() {
        return this.<String>value(DecisionGraphStateKey.QUERY_PLANNING_KEY).orElse("");
    }

    public String answer() {
        return this.<String>value(DecisionGraphStateKey.ANSWER_KEY)
                .orElseThrow(() -> new IllegalStateException("Answer missing from graph state"));
    }
}
