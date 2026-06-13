package com.app.decisioniq.agent.node.decison;

import com.app.decisioniq.agent.constant.AgentConstants;
import com.app.decisioniq.agent.type.DecisionIQAgent;
import com.app.decisioniq.service.agent.DecisionIntentService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DIQ_IntentUnderstandingAgent implements NodeAction<DecisionIQAgent> {

    private final DecisionIntentService decisionIntentService;

    @Autowired
    public DIQ_IntentUnderstandingAgent(DecisionIntentService decisionIntentService) {
        this.decisionIntentService = decisionIntentService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgent state) {
        String response = decisionIntentService.validateIntentAndAction(state.intent());
        log.info("response received is {}",response);
        return Map.of(AgentConstants.ANSWER,response);
    }
}
