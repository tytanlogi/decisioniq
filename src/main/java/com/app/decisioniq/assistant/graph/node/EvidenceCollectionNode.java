package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import com.app.decisioniq.service.evidence.EvidenceService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EvidenceCollectionNode implements NodeAction<DecisionIQAgentState> {

    private final EvidenceService evidenceService;

    @Autowired
    public EvidenceCollectionNode(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        QueryPlan queryPlan = state.getQuery()
                .orElseThrow(() -> new IllegalStateException("QueryPlan missing before evidence collection"));
        return Map.of(DecisionGraphStateKey.EVIDENCE_COLLECTION_KEY,evidenceService.initiateEvidenceCollectionMechanism(queryPlan));
    }
}
