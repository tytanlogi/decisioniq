package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.service.planning.QueryPlanningService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QueryPlanningNode implements NodeAction<DecisionIQAgentState> {

    private final QueryPlanningService queryPlanningService;

    public QueryPlanningNode(QueryPlanningService queryPlanningService) {
        this.queryPlanningService = queryPlanningService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        String queryResult = queryPlanningService.planQuery(state.parsedQuestionJson());
        return Map.of(DecisionGraphStateKey.QUERY_RESULT, queryResult);
    }
}
