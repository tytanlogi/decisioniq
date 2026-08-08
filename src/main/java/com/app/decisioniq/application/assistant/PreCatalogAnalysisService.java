package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.nlp.NlpAnalysis;
import com.app.decisioniq.application.nlp.NlpAnalyzer;
import com.app.decisioniq.application.nlp.NlpClauseRoleAnalyzer;
import com.app.decisioniq.application.nlp.NlpContextCandidate;
import com.app.decisioniq.application.nlp.NlpOperationAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.application.nlp.NlpRequestGroup;
import com.app.decisioniq.application.nlp.NlpRequestGroupAnalyzer;
import com.app.decisioniq.application.nlp.NlpTransactionReferenceExtractor;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the complete deterministic request path through the exact point at which catalog retrieval
 * would begin. This service never calls the catalog, Milvus, OpenAI, or a database.
 */
@Service
public class PreCatalogAnalysisService {

    private final AssistantRequestBoundary requestBoundary;
    private final NlpAnalyzer nlpAnalyzer;
    private final NlpOperationAnalyzer operationAnalyzer;
    private final NlpClauseRoleAnalyzer clauseRoleAnalyzer;
    private final NlpRequestGroupAnalyzer requestGroupAnalyzer;

    public PreCatalogAnalysisService(
            AssistantRequestBoundary requestBoundary,
            NlpAnalyzer nlpAnalyzer,
            NlpOperationAnalyzer operationAnalyzer,
            NlpClauseRoleAnalyzer clauseRoleAnalyzer,
            NlpRequestGroupAnalyzer requestGroupAnalyzer
    ) {
        this.requestBoundary = requestBoundary;
        this.nlpAnalyzer = nlpAnalyzer;
        this.operationAnalyzer = operationAnalyzer;
        this.clauseRoleAnalyzer = clauseRoleAnalyzer;
        this.requestGroupAnalyzer = requestGroupAnalyzer;
    }

    public PreCatalogAnalysisResult analyze(AssistantRequestCommand command) {
        GuardrailDecision guardrailDecision = requestBoundary.evaluate(command);
        if (guardrailDecision.outcome() != GuardrailOutcome.ALLOW_TO_INTERPRET) {
            return new PreCatalogAnalysisResult(
                    PreCatalogDisposition.BLOCKED_BY_GUARDRAIL,
                    guardrailDecision,
                    List.of(),
                    List.of(),
                    null
            );
        }

        NlpAnalysis analysis = nlpAnalyzer.analyze(guardrailDecision.normalizedQuestion());
        List<NlpOperationFrame> frames = clauseRoleAnalyzer.classify(
                analysis,
                operationAnalyzer.analyze(analysis)
        );
        if (frames.stream().anyMatch(NlpOperationFrame::blocksRequest)) {
            return new PreCatalogAnalysisResult(
                    PreCatalogDisposition.BLOCKED_BY_OPERATION_POLICY,
                    guardrailDecision,
                    frames,
                    List.of(),
                    null
            );
        }
        List<NlpRequestGroup> groups = requestGroupAnalyzer.group(guardrailDecision.normalizedQuestion(), frames);
        if (groups.stream().anyMatch(group -> group.kind() == NlpRequestGroup.Kind.CLARIFY)) {
            return new PreCatalogAnalysisResult(
                    PreCatalogDisposition.CLARIFY_REQUIRED,
                    guardrailDecision,
                    frames,
                    List.of(),
                    groups.getFirst().comparisonAspect()
            );
        }
        if (frames.stream().noneMatch(NlpOperationFrame::executable)) {
            return new PreCatalogAnalysisResult(
                    PreCatalogDisposition.IGNORED_HARMLESS_NOISE,
                    guardrailDecision,
                    frames,
                    List.of(),
                    null
            );
        }
        return new PreCatalogAnalysisResult(
                PreCatalogDisposition.READY_FOR_CATALOG,
                guardrailDecision,
                frames,
                searchRequests(guardrailDecision.normalizedQuestion(), frames, groups),
                null
        );
    }

    private List<PreCatalogSearchRequest> searchRequests(
            String normalizedQuestion,
            List<NlpOperationFrame> frames,
            List<NlpRequestGroup> groups
    ) {
        List<PreCatalogSearchRequest> requests = new ArrayList<>();
        ResolvedLocalContext activeContext = null;
        if (groups.stream().anyMatch(group -> group.kind() == NlpRequestGroup.Kind.COMPARISON)) {
            NlpRequestGroup group = groups.getFirst();
            List<PreCatalogLookupUnit> lookups = group.unitIndexes().stream()
                    .map(index -> lookupUnit(index, frames.get(index)))
                    .toList();
            return List.of(new PreCatalogSearchRequest(
                    group.unitIndexes().getFirst(),
                    normalizedQuestion,
                    null,
                    "COMPARISON",
                    lookups,
                    group.comparisonAspect()
            ));
        }
        for (int index = 0; index < frames.size(); index++) {
            NlpOperationFrame frame = frames.get(index);
            NlpContextCandidate candidate = frame.contextCandidate();
            if (candidate != null) {
                activeContext = new ResolvedLocalContext(
                        candidate.transactionId(),
                        candidate.assertedOutcome(),
                        index
                );
            }
            if (frame.executable()) {
                requests.add(new PreCatalogSearchRequest(
                        index,
                        frame.text(),
                        activeContext,
                        "LOOKUP",
                        List.of(lookupUnit(index, frame)),
                        null
                ));
                // The ID in an explicit lookup may be referenced by a following "it/this"
                // lookup, but it is not treated as a context statement for the current query.
                if (candidate == null && !NlpTransactionReferenceExtractor.all(frame.text()).isEmpty()) {
                    activeContext = new ResolvedLocalContext(
                            frame.resolvedTransactionId(),
                            null,
                            index
                    );
                }
            }
        }
        return List.copyOf(requests);
    }

    private PreCatalogLookupUnit lookupUnit(int index, NlpOperationFrame frame) {
        List<String> transactionIds = NlpTransactionReferenceExtractor.all(frame.text());
        return new PreCatalogLookupUnit(
                index,
                frame.text(),
                frame.resolvedTransactionId(),
                transactionIds
        );
    }
}
