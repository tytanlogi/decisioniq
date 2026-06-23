package com.app.decisioniq.assistant.planning.catalog;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;

import java.util.List;
import java.util.Map;

/**
 * Maps an assistant intent to the RAG chunk types useful for semantic evidence retrieval.
 */
public final class IntentToRagChunkTypeMap {

    private IntentToRagChunkTypeMap() {
    }

    private static final Map<DecisionIqIntent, List<String>> INTENT_TO_CHUNK_TYPES = Map.of(
            DecisionIqIntent.EXPLAIN_TRANSACTION_DECISION,
            List.of(
                    "DECISION_CASE_SUMMARY",
                    "MODEL_SCORE_RISK",
                    "TRANSACTION_RISK_SIGNALS"
            )
    );

    public static List<String> chunkTypesFor(DecisionIqIntent intent) {
        return INTENT_TO_CHUNK_TYPES.getOrDefault(intent, List.of());
    }
}
