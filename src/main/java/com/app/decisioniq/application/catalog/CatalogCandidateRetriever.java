package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.infrastructure.catalog.CatalogClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogCandidateRetriever {

    private final CatalogClient catalogClient;

    public CatalogCandidateRetriever(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    public List<UnitCatalogCandidates> retrieve(
            List<NlpOperationFrame> units,
            String correlationId
    ) {
        return units.stream()
                .map(unit -> new UnitCatalogCandidates(
                        unit,
                        catalogClient.search(unit.text(), correlationId)
                ))
                .toList();
    }
}
