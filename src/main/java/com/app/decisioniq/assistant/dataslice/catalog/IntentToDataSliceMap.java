package com.app.decisioniq.assistant.dataslice.catalog;

import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;

import java.util.List;
import java.util.Map;

/**
 * Maps an assistant intent to the business data slices required to answer it.
 */
public final class IntentToDataSliceMap {

    private IntentToDataSliceMap() {
    }

    private static final Map<DecisionIqIntent, List<DecisionDataSlice>> INTENT_TO_DATA_SLICES = Map.of(
            DecisionIqIntent.EXPLAIN_TRANSACTION_DECISION,
            List.of(
                    DecisionDataSlice.TRANSACTION_DECISION_SLICE,
                    DecisionDataSlice.DECISION_EXPLANATION_SLICE
            )
    );

    public static List<DecisionDataSlice> slicesFor(DecisionIqIntent intent) {
        return INTENT_TO_DATA_SLICES.getOrDefault(intent, List.of());
    }
}
