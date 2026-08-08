package com.app.decisioniq.application.catalog;

import com.app.decisioniq.config.catalog.CatalogClientProperties;
import com.app.decisioniq.domain.catalog.CatalogCandidate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CatalogCandidateClassifier {

    private static final Comparator<CatalogCandidate> BY_SCORE_DESCENDING =
            Comparator.comparingDouble(CatalogCandidate::score).reversed();

    private final CatalogClientProperties properties;

    public CatalogCandidateClassifier(CatalogClientProperties properties) {
        this.properties = properties;
    }

    public CatalogUnitPartition classify(List<UnitCatalogCandidates> retrievedUnits) {
        List<SupportedCatalogUnit> supported = new ArrayList<>();
        List<UnsupportedCatalogUnit> unsupported = new ArrayList<>();

        for (UnitCatalogCandidates retrievedUnit : retrievedUnits) {
            List<CatalogCandidate> ranked = retrievedUnit.candidates().stream()
                    .sorted(BY_SCORE_DESCENDING)
                    .toList();
            if (ranked.isEmpty()) {
                unsupported.add(new UnsupportedCatalogUnit(
                        retrievedUnit.unit(),
                        UnsupportedCatalogReason.NO_CAPABILITY_MATCH,
                        null
                ));
                continue;
            }

            if (ranked.getFirst().score() < properties.minimumSupportedScore()) {
                unsupported.add(new UnsupportedCatalogUnit(
                        retrievedUnit.unit(),
                        UnsupportedCatalogReason.NO_CAPABILITY_MATCH,
                        ranked.getFirst().score()
                ));
                continue;
            }

            supported.add(new SupportedCatalogUnit(retrievedUnit.unit(), ranked));
        }

        return new CatalogUnitPartition(supported, unsupported);
    }
}
