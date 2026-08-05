package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
import com.app.decisioniq.domain.catalog.CatalogRelevanceOutcome;

import java.util.List;

public record CatalogFrameValidation(
        CatalogRelevanceDecision decision,
        List<UnitValidation> units,
        String effectiveQuestion
) {
    public CatalogFrameValidation {
        units = List.copyOf(units);
    }

    public record UnitValidation(
            NlpOperationFrame frame,
            CatalogRelevanceOutcome catalogOutcome,
            List<Match> matches,
            boolean includedInEffectiveQuestion
    ) {
        public UnitValidation {
            matches = List.copyOf(matches);
        }
    }
}
