package com.app.decisioniq.application.catalog;

import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.config.catalog.CatalogRelevanceProperties;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
import com.app.decisioniq.domain.catalog.CatalogRelevanceOutcome;
import com.app.decisioniq.infrastructure.catalog.CatalogClient;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogRelevanceService {

    private static final int MAX_AGGREGATED_MATCHES = 5;

    private final CatalogClient catalogClient;
    private final CatalogRelevanceProperties properties;

    public CatalogRelevanceService(
            CatalogClient catalogClient,
            CatalogRelevanceProperties properties
    ) {
        this.catalogClient = catalogClient;
        this.properties = properties;
    }

    public CatalogRelevanceDecision evaluate(String question, String correlationId) {
        List<Match> matches = catalogClient.search(question, correlationId);
        if (matches.isEmpty()) {
            return decision(lowSignalOutcome(question), matches);
        }

        double bestScore = matches.getFirst().score();
        if (bestScore >= properties.supportedScore()) {
            return decision(CatalogRelevanceOutcome.SUPPORTED, matches);
        }
        if (bestScore >= properties.ambiguousScore()) {
            return decision(CatalogRelevanceOutcome.AMBIGUOUS, matches);
        }
        return decision(lowSignalOutcome(question), matches);
    }

    public CatalogRelevanceDecision evaluateUnits(List<String> units, String correlationId) {
        if (units == null || units.isEmpty()) {
            throw new IllegalArgumentException("At least one request unit is required");
        }

        List<CatalogRelevanceDecision> decisions = units.stream()
                .map(unit -> evaluate(unit, correlationId))
                .toList();

        List<CatalogRelevanceDecision> supported = decisions.stream()
                .filter(decision -> decision.outcome() == CatalogRelevanceOutcome.SUPPORTED)
                .toList();
        if (!supported.isEmpty()) {
            return decision(CatalogRelevanceOutcome.SUPPORTED, aggregateMatches(supported));
        }
        if (decisions.stream().anyMatch(
                decision -> decision.outcome() == CatalogRelevanceOutcome.AMBIGUOUS
        )) {
            return decision(CatalogRelevanceOutcome.AMBIGUOUS, aggregateMatches(decisions));
        }
        if (decisions.stream().allMatch(
                decision -> decision.outcome() == CatalogRelevanceOutcome.INSUFFICIENT_CONTEXT
        )) {
            return decision(CatalogRelevanceOutcome.INSUFFICIENT_CONTEXT, aggregateMatches(decisions));
        }
        return decision(CatalogRelevanceOutcome.OUT_OF_SCOPE, aggregateMatches(decisions));
    }

    public CatalogRelevanceDecision evaluateFrames(
            List<NlpOperationFrame> frames,
            String correlationId
    ) {
        return validateFrames(frames, correlationId).decision();
    }

    public CatalogFrameValidation validateFrames(
            List<NlpOperationFrame> frames,
            String correlationId
    ) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("At least one operation frame is required");
        }

        List<FrameDecision> decisions = frames.stream()
                .map(frame -> new FrameDecision(
                        frame,
                        evaluate(frame.text(), correlationId)
                ))
                .toList();

        CatalogRelevanceDecision aggregate = aggregateFrameDecision(decisions);
        boolean canBuildEffectiveQuestion = aggregate.outcome()
                == CatalogRelevanceOutcome.SUPPORTED;
        List<CatalogFrameValidation.UnitValidation> units = decisions.stream()
                .map(frameDecision -> new CatalogFrameValidation.UnitValidation(
                        frameDecision.frame(),
                        frameDecision.decision().outcome(),
                        frameDecision.decision().matches(),
                        canBuildEffectiveQuestion
                                && frameDecision.decision().outcome()
                                == CatalogRelevanceOutcome.SUPPORTED
                ))
                .toList();
        String effectiveQuestion = canBuildEffectiveQuestion
                ? units.stream()
                .filter(CatalogFrameValidation.UnitValidation::includedInEffectiveQuestion)
                .map(unit -> unit.frame().text())
                .collect(java.util.stream.Collectors.joining(" "))
                : null;
        return new CatalogFrameValidation(aggregate, units, effectiveQuestion);
    }

    private CatalogRelevanceDecision aggregateFrameDecision(List<FrameDecision> decisions) {
        List<CatalogRelevanceDecision> supported = decisions.stream()
                .map(FrameDecision::decision)
                .filter(decision -> decision.outcome() == CatalogRelevanceOutcome.SUPPORTED)
                .toList();
        if (!supported.isEmpty()) {
            return decision(CatalogRelevanceOutcome.SUPPORTED, aggregateMatches(supported));
        }
        List<CatalogRelevanceDecision> allDecisions = decisions.stream()
                .map(FrameDecision::decision)
                .toList();
        if (allDecisions.stream().anyMatch(
                decision -> decision.outcome() == CatalogRelevanceOutcome.AMBIGUOUS
        )) {
            return decision(CatalogRelevanceOutcome.AMBIGUOUS, aggregateMatches(allDecisions));
        }
        if (allDecisions.stream().allMatch(
                decision -> decision.outcome() == CatalogRelevanceOutcome.INSUFFICIENT_CONTEXT
        )) {
            return decision(
                    CatalogRelevanceOutcome.INSUFFICIENT_CONTEXT,
                    aggregateMatches(allDecisions)
            );
        }
        return decision(CatalogRelevanceOutcome.OUT_OF_SCOPE, aggregateMatches(allDecisions));
    }

    private List<Match> aggregateMatches(List<CatalogRelevanceDecision> decisions) {
        Map<String, Match> bestByCatalogVersion = new LinkedHashMap<>();
        decisions.stream()
                .flatMap(decision -> decision.matches().stream())
                .forEach(match -> bestByCatalogVersion.merge(
                        match.catalogKey() + ":" + match.version(),
                        match,
                        (left, right) -> left.score() >= right.score() ? left : right
                ));
        return bestByCatalogVersion.values().stream()
                .sorted(Comparator.comparingDouble(Match::score).reversed())
                .limit(MAX_AGGREGATED_MATCHES)
                .toList();
    }

    private CatalogRelevanceOutcome lowSignalOutcome(String question) {
        long terms = java.util.regex.Pattern.compile("[\\p{L}\\p{N}]+")
                .matcher(question)
                .results()
                .count();
        if (terms > 0 && terms <= properties.lowInformationTermLimit()) {
            return CatalogRelevanceOutcome.INSUFFICIENT_CONTEXT;
        }
        return CatalogRelevanceOutcome.OUT_OF_SCOPE;
    }

    private CatalogRelevanceDecision decision(
            CatalogRelevanceOutcome outcome,
            List<Match> matches
    ) {
        return new CatalogRelevanceDecision(outcome, matches);
    }

    private record FrameDecision(
            NlpOperationFrame frame,
            CatalogRelevanceDecision decision
    ) { }
}
