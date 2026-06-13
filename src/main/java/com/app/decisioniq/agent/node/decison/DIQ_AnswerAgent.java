package com.app.decisioniq.agent.node.decison;

import com.app.decisioniq.agent.type.DecisionIQAgent;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DIQ_AnswerAgent implements NodeAction<DecisionIQAgent> {

    @Override
    public Map<String, Object> apply(DecisionIQAgent state) throws Exception {
        log.info("Processing the answer");
        return Map.of();
    }
}
