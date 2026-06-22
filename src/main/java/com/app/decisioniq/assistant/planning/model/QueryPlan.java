package com.app.decisioniq.assistant.planning.model;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import lombok.Data;

import java.util.List;

/**
 * Internal data-getQuery plan produced by getQuery planning.
 *
 * <p>The plan tells the downstream evidence fetch nodes what SQL evidence and RAG evidence are
 * required for the parsed user intent. It does not execute SQL, call Milvus, or call the LLM.</p>
 */
@Data
public class QueryPlan {

    private String tenantId;
    private String transactionId;
    private List<DecisionIqIntent> intents;
    private SqlData sql;
    private RagData rag;
}
