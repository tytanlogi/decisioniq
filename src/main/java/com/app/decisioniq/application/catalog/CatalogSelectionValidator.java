package com.app.decisioniq.application.catalog;

import com.app.decisioniq.domain.catalog.CatalogCandidate;
import com.app.decisioniq.domain.catalog.CatalogSelection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogSelectionValidator {

    public CatalogSelectionValidation validate(
            List<CatalogSelection> selections,
            List<UnitCatalogCandidates> retrievedCandidates
    ) {
        Map<CatalogIdentity, CatalogCandidate> approvedCandidates = new LinkedHashMap<>();
        retrievedCandidates.stream()
                .flatMap(unit -> unit.candidates().stream())
                .forEach(candidate -> approvedCandidates.putIfAbsent(
                        new CatalogIdentity(candidate.catalogKey(), candidate.version()),
                        candidate
                ));

        List<CatalogCandidate> approved = new ArrayList<>();
        List<CatalogSelection> rejected = new ArrayList<>();
        for (CatalogSelection selection : selections) {
            CatalogCandidate candidate = approvedCandidates.get(
                    new CatalogIdentity(selection.catalogKey(), selection.version())
            );
            if (candidate == null) {
                rejected.add(selection);
            } else {
                approved.add(candidate);
            }
        }
        return new CatalogSelectionValidation(approved, rejected);
    }

    private record CatalogIdentity(String catalogKey, int version) { }
}
