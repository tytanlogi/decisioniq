package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import com.app.decisioniq.service.planning.QueryPlanningService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class QueryPlanningNode implements NodeAction<DecisionIQAgentState> {

    private final QueryPlanningService queryPlanningService;

    public QueryPlanningNode(QueryPlanningService queryPlanningService) {
        this.queryPlanningService = queryPlanningService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        QueryPlan queryPlan = buildQueryPlan(state);
        return Map.of(DecisionGraphStateKey.QUERY_PLANNING_KEY,queryPlan);
    }
    private QueryPlan buildQueryPlan(DecisionIQAgentState state) {
        ParsedQuestion parsedQuestion = requireParsedQuestion(state);
        return queryPlanningService.planQuery(parsedQuestion, state.tenantId());
    }

    private ParsedQuestion requireParsedQuestion(DecisionIQAgentState state) {
        return state.clarifyIntent()
                .orElseThrow(() -> new IllegalStateException(
                        "Parsed question missing before query planning. Check graph routing from IntentUnderstandingNode."
                ));
    }}
