package com.app.decisioniq.application.nlp;

import java.util.List;

/**
 * A deterministic grouping of executable clauses that must remain together for catalog routing.
 */
public record NlpRequestGroup(
        Kind kind,
        List<Integer> unitIndexes,
        String comparisonAspect
) {
    public NlpRequestGroup {
        unitIndexes = List.copyOf(unitIndexes);
    }

    public enum Kind {
        LOOKUP,
        COMPARISON,
        CLARIFY
    }
}
