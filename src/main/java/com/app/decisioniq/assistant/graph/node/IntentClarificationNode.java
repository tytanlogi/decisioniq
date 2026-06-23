package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class IntentClarificationNode implements NodeAction<DecisionIQAgentState> {

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        String message="Cannot answer the question, unable to find the intent";
        Optional<ParsedQuestion> intentClarification = state.clarifyIntent();
        if (intentClarification.isPresent()){
            message=intentClarification.get().getClarificationQuestion();
        }
        return Map.of(DecisionGraphStateKey.ANSWER_KEY,message);
    }
}
