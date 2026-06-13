package com.app.decisioniq.agent.node.decison;

import com.app.decisioniq.agent.constant.AgentConstants;
import com.app.decisioniq.agent.type.DecisionIQAgent;
import com.app.decisioniq.service.agent.DecisionQueryService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DIQ_QueryAgent implements NodeAction<DecisionIQAgent> {

    private final DecisionQueryService decisionQueryService;

    @Autowired
    public DIQ_QueryAgent(DecisionQueryService decisionQueryService) {
        this.decisionQueryService = decisionQueryService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgent state) throws Exception {
        String queryAnswered = decisionQueryService.startQuery(state.query());
        return Map.of(AgentConstants.QUERY,queryAnswered);
    }
}
