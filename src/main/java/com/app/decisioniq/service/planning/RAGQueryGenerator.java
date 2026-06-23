package com.app.decisioniq.service.planning;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import com.app.decisioniq.assistant.planning.model.QueryPlan;

import java.util.List;

public class RAGQueryGenerator {

    public static void buildRAGQueryPlan(List<String> ragChunkList, QueryPlan queryPlan, DecisionIqIntent intent){
        queryPlan.setRagCollectionName("decisioniq_evidence_chunks");
        queryPlan.getRagMap().put(intent.name(),ragChunkList);
    }

}
