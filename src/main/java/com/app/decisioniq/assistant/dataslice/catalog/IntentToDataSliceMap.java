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

    private static final List<DecisionDataSlice> DECISION_EXPLANATION_SLICES = List.of(
            DecisionDataSlice.TRANSACTION_DECISION_SLICE,
            DecisionDataSlice.DECISION_EXPLANATION_SLICE
    );

    private static final List<DecisionDataSlice> TRANSACTION_SUMMARY_SLICES = List.of(
            DecisionDataSlice.TRANSACTION_DECISION_SLICE,
            DecisionDataSlice.CUSTOMER_CONTEXT_SLICE,
            DecisionDataSlice.ACCOUNT_CONTEXT_SLICE
    );

    private static final List<DecisionDataSlice> RISK_EXPLANATION_SLICES = List.of(
            DecisionDataSlice.TRANSACTION_CONTEXT_SLICE,
            DecisionDataSlice.MODEL_SCORE_SLICE,
            DecisionDataSlice.RISK_SIGNAL_SLICE,
            DecisionDataSlice.CUSTOMER_CONTEXT_SLICE,
            DecisionDataSlice.ACCOUNT_CONTEXT_SLICE
    );

    private static final List<DecisionDataSlice> SIMILAR_CASE_ANCHOR_SLICES = List.of(
            DecisionDataSlice.TRANSACTION_DECISION_SLICE,
            DecisionDataSlice.MODEL_SCORE_SLICE,
            DecisionDataSlice.RISK_SIGNAL_SLICE,
            DecisionDataSlice.CUSTOMER_CONTEXT_SLICE
    );

    private static final Map<DecisionIqIntent, List<DecisionDataSlice>> INTENT_TO_DATA_SLICES = Map.ofEntries(
            Map.entry(DecisionIqIntent.EXPLAIN_TRANSACTION_DECISION, DECISION_EXPLANATION_SLICES),
            Map.entry(DecisionIqIntent.EXPLAIN_APPROVAL, DECISION_EXPLANATION_SLICES),
            Map.entry(DecisionIqIntent.EXPLAIN_DECLINE, DECISION_EXPLANATION_SLICES),
            Map.entry(DecisionIqIntent.EXPLAIN_REVIEW, DECISION_EXPLANATION_SLICES),

            Map.entry(DecisionIqIntent.SHOW_TRANSACTION_SUMMARY, TRANSACTION_SUMMARY_SLICES),
            Map.entry(DecisionIqIntent.SHOW_MODEL_SCORE, List.of(DecisionDataSlice.MODEL_SCORE_SLICE)),
            Map.entry(DecisionIqIntent.SHOW_MODEL_SCORE_THRESHOLDS, List.of(DecisionDataSlice.MODEL_SCORE_SLICE)),
            Map.entry(DecisionIqIntent.SHOW_RULE_FIRED, List.of(DecisionDataSlice.RULE_EVALUATION_SLICE)),
            Map.entry(DecisionIqIntent.SHOW_REASON_CODES, List.of(DecisionDataSlice.REASON_CODE_SLICE)),
            Map.entry(DecisionIqIntent.SHOW_RISK_BAND, List.of(DecisionDataSlice.MODEL_SCORE_SLICE)),

            Map.entry(DecisionIqIntent.SHOW_CUSTOMER_HISTORY, List.of(DecisionDataSlice.CUSTOMER_CONTEXT_SLICE)),
            Map.entry(DecisionIqIntent.COMPARE_WITH_CUSTOMER_HISTORY, List.of(
                    DecisionDataSlice.TRANSACTION_CONTEXT_SLICE,
                    DecisionDataSlice.CUSTOMER_CONTEXT_SLICE,
                    DecisionDataSlice.ACCOUNT_CONTEXT_SLICE
            )),

            Map.entry(DecisionIqIntent.SHOW_DMP_TRACE, List.of(DecisionDataSlice.DMP_TRACE_SLICE)),
            Map.entry(DecisionIqIntent.SHOW_GENERATED_FEATURES, List.of(DecisionDataSlice.GENERATED_FEATURE_SLICE)),

            Map.entry(DecisionIqIntent.FIND_SIMILAR_CASES, SIMILAR_CASE_ANCHOR_SLICES),
            Map.entry(DecisionIqIntent.FIND_SUSPICIOUS_APPROVALS, SIMILAR_CASE_ANCHOR_SLICES),

            Map.entry(DecisionIqIntent.EXPLAIN_RISK_SIGNALS, RISK_EXPLANATION_SLICES),
            Map.entry(DecisionIqIntent.EXPLAIN_RULE, List.of(DecisionDataSlice.RULE_EVALUATION_SLICE)),
            Map.entry(DecisionIqIntent.EXPLAIN_POLICY, List.of(DecisionDataSlice.RULE_EVALUATION_SLICE))
    );

    public static List<DecisionDataSlice> slicesFor(DecisionIqIntent intent) {
        return INTENT_TO_DATA_SLICES.getOrDefault(intent, List.of());
    }
}
