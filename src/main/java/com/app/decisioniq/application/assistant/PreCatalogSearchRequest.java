package com.app.decisioniq.application.assistant;

/**
 * The versioned logical payload ready to be sent to catalog retrieval.
 * The query is embedded and searched; context remains typed metadata for Java.
 */
public record PreCatalogSearchRequest(
        int unitIndex,
        String query,
        ResolvedLocalContext resolvedContext,
        String requestKind,
        java.util.List<PreCatalogLookupUnit> lookupUnits,
        String comparisonAspect
) {
    public PreCatalogSearchRequest {
        lookupUnits = java.util.List.copyOf(lookupUnits);
    }
}
