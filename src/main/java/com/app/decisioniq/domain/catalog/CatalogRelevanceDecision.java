package com.app.decisioniq.domain.catalog;

import java.util.List;

public record CatalogRelevanceDecision(
        CatalogRelevanceOutcome outcome,
        List<Match> matches
) {
    public CatalogRelevanceDecision {
        matches = List.copyOf(matches);
    }

    public record Match(String catalogKey, int version, double score) { }
}
