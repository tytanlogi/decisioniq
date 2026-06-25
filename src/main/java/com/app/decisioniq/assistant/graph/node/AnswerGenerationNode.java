package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.evidence.model.ExtractedData;
import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import com.app.decisioniq.service.answer.AnswerGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

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
        Optional<ExtractedData> extractedData = state.collectEvidence();
        Optional<QueryPlan> query = state.getQuery();
        Optional<ParsedQuestion> parsedQuestion = state.intent();
        if (extractedData.isPresent() && query.isPresent() && parsedQuestion.isPresent()){
            String answer = answerGenerationService.generateAnswer(
                    state.question(),
                    parsedQuestion.get(),
                    query.get(),
                    extractedData.get()
            );
            return Map.of(ANSWER_KEY, answer);
        }
        return Map.of(ANSWER_KEY,"Unable to generate an answer because query planning or evidence collection is missing.");
    }
}
