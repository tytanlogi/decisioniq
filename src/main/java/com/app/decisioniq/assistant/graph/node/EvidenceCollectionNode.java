package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import com.app.decisioniq.service.evidence.EvidenceService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class EvidenceCollectionNode implements NodeAction<DecisionIQAgentState> {

    private final EvidenceService evidenceService;

    @Autowired
    public EvidenceCollectionNode(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        Optional<QueryPlan> query = state.getQuery();
        query.ifPresent(evidenceService::initiateEvidenceCollectionMechanism);
        return Map.of();
    }
}
