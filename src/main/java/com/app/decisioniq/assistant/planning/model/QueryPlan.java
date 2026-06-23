package com.app.decisioniq.assistant.planning.model;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal data-getQuery plan produced by getQuery planning.
 *
 * <p>The plan tells the downstream evidence fetch nodes what SQL evidence and RAG evidence are
 * required for the parsed user intent. It does not execute SQL, call Milvus, or call the LLM.</p>
 */
@Data
public class QueryPlan implements Serializable {

    private String tenantId;
    private String transactionId;
    private List<DecisionIqIntent> intents;
    private String ragCollectionName;
    private Map<String,MultiValueMap<String,List<String >>> sqlTableFieldsMap =new HashMap<>();
    private Map<String,List<String>> ragMap=new HashMap<>();
}
