package com.app.decisioniq.application.assistant;

/**
 * One executable clause contained in a logical pre-catalog request.
 */
public record PreCatalogLookupUnit(
        int unitIndex,
        String text,
        String transactionId,
        java.util.List<String> transactionIds
) {
    public PreCatalogLookupUnit {
        transactionIds = java.util.List.copyOf(transactionIds);
    }
}
