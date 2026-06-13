package com.app.decisioniq.agent.node.decison;

import com.app.decisioniq.agent.constant.AgentConstants;
import com.app.decisioniq.agent.type.DecisionIQAgent;
import com.app.decisioniq.service.agent.DecisionAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DIQ_AnswerAgent implements NodeAction<DecisionIQAgent> {

    private final DecisionAnswerService decisionAnswerService;

    @Autowired
    public DIQ_AnswerAgent(DecisionAnswerService decisionAnswerService) {
        this.decisionAnswerService = decisionAnswerService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgent state) throws Exception {
        log.info("Processing the answer");
        String answer = decisionAnswerService.answer();
        return Map.of(AgentConstants.ANSWER,answer);
    }
}
