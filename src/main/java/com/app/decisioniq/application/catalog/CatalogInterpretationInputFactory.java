package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.assistant.InterpretationInput;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogInterpretationInputFactory {

    private static final String SCHEMA_VERSION = "1.1";

    private final CatalogCandidateRetriever candidateRetriever;
    private final CatalogCandidateClassifier candidateClassifier;

    /**
     * Creates an input factory from catalog retrieval and candidate classification responsibilities.
     */
    public CatalogInterpretationInputFactory(
            CatalogCandidateRetriever candidateRetriever,
            CatalogCandidateClassifier candidateClassifier
    ) {
        this.candidateRetriever = candidateRetriever;
        this.candidateClassifier = candidateClassifier;
    }

    /**
     * Retrieves and classifies catalog candidates, then builds the bounded interpretation input.
     */
    public InterpretationInput create(
            String normalizedQuestion,
            List<NlpOperationFrame> frames,
            String correlationId
    ) {
        // Search the external semantic catalog independently for every decomposed request unit.
        List<UnitCatalogCandidates> candidates = candidateRetriever.retrieve(frames, correlationId);

        // Separate units with sufficient capability evidence from units that have no reliable match.
        CatalogUnitPartition partition = candidateClassifier.classify(candidates);

        // Build the versioned, bounded contract that the interpretation model is allowed to inspect.
        return new InterpretationInput(
                SCHEMA_VERSION,
                normalizedQuestion,
                partition.supportedUnits(),
                partition.unsupportedUnits()
        );
    }
}
