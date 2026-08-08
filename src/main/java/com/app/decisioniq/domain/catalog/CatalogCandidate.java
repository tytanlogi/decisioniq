package com.app.decisioniq.domain.catalog;

public record CatalogCandidate(
        String catalogKey,
        int version,
        double score
) { }
