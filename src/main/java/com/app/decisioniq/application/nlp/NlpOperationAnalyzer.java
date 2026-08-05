package com.app.decisioniq.application.nlp;

import com.app.decisioniq.config.nlp.OperationPolicyProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NlpOperationAnalyzer {

    private final OperationPolicyProperties properties;

    public NlpOperationAnalyzer(OperationPolicyProperties properties) {
        this.properties = properties;
    }

    public List<NlpOperationFrame> analyze(NlpAnalysis analysis) {
        List<NlpOperationFrame> frames = new ArrayList<>();
        for (NlpAnalysis.Sentence sentence : analysis.sentences()) {
            for (NlpAnalysis.Clause clause : sentence.clauses()) {
                frames.add(frame(sentence, clause));
            }
        }
        return List.copyOf(frames);
    }

    private NlpOperationFrame frame(
            NlpAnalysis.Sentence sentence,
            NlpAnalysis.Clause clause
    ) {
        List<NlpAnalysis.Token> tokens = sentence.tokens().stream()
                .filter(token -> token.index() >= clause.firstTokenIndex())
                .filter(token -> token.index() <= clause.lastTokenIndex())
                .toList();
        Set<Integer> tokenIndexes = tokens.stream()
                .map(NlpAnalysis.Token::index)
                .collect(Collectors.toSet());
        List<NlpAnalysis.Dependency> dependencies = sentence.dependencies().stream()
                .filter(dependency -> tokenIndexes.contains(dependency.dependentIndex()))
                .toList();

        Set<Integer> rootIndexes = dependencies.stream()
                .filter(dependency -> "root".equals(dependency.relation()))
                .map(NlpAnalysis.Dependency::dependentIndex)
                .collect(java.util.stream.Collectors.toSet());
        Set<Integer> inferredActionIndexes = new HashSet<>();
        List<NlpAnalysis.Token> actionTokens = new ArrayList<>(tokens.stream()
                .filter(token -> isVerb(token)
                        || rootIndexes.contains(token.index()) && isKnownAction(token.lemma()))
                .toList());
        if (actionTokens.isEmpty()
                && !tokens.isEmpty()
                && "JJ".equals(tokens.getFirst().partOfSpeech())) {
            actionTokens.add(tokens.getFirst());
            inferredActionIndexes.add(tokens.getFirst().index());
        }
        List<String> actions = distinct(actionTokens.stream()
                .map(NlpAnalysis.Token::lemma)
                .toList());
        List<String> objects = distinct(dependencies.stream()
                .filter(dependency -> isObjectRelation(dependency.relation()))
                .map(NlpAnalysis.Dependency::dependentIndex)
                .map(index -> token(tokens, index))
                .filter(Objects::nonNull)
                .map(NlpAnalysis.Token::lemma)
                .toList());

        List<String> nounLemmas = tokens.stream()
                .filter(token -> token.partOfSpeech() != null)
                .filter(token -> token.partOfSpeech().startsWith("NN"))
                .map(NlpAnalysis.Token::lemma)
                .map(this::normalize)
                .toList();
        List<String> targets = nounLemmas.stream()
                .filter(this::isKnownTarget)
                .distinct()
                .toList();

        Classification classification = classify(
                tokens, actionTokens, rootIndexes, inferredActionIndexes, targets
        );
        return new NlpOperationFrame(
                sentence.index(),
                clause.index(),
                clause.text(),
                actions,
                objects,
                targets,
                classification.effect(),
                classification.certainty()
        );
    }

    private Classification classify(
            List<NlpAnalysis.Token> tokens,
            List<NlpAnalysis.Token> actionTokens,
            Set<Integer> rootIndexes,
            Set<Integer> inferredActionIndexes,
            List<String> targets
    ) {
        Set<String> requestedActions = actionTokens.stream()
                .filter(token -> canRequestOperation(
                        token, rootIndexes, inferredActionIndexes
                ))
                .map(NlpAnalysis.Token::lemma)
                .map(this::normalize)
                .collect(Collectors.toSet());
        boolean question = tokens.stream().anyMatch(token ->
                token.partOfSpeech() != null && token.partOfSpeech().startsWith("W")
        );
        boolean persistentTarget = intersects(targets, properties.targets().persistent());
        boolean presentationTarget = intersects(targets, properties.targets().presentation());
        boolean domainDataTarget = intersects(targets, properties.targets().domainData());

        if (intersects(requestedActions, properties.actions().modify())) {
            return explicit(NlpOperationFrame.Effect.MODIFY);
        }
        if (intersects(requestedActions, properties.actions().external())) {
            return explicit(NlpOperationFrame.Effect.EXTERNAL_ACTION);
        }
        if (intersects(requestedActions, properties.actions().transfer())) {
            return explicit(NlpOperationFrame.Effect.TRANSFER);
        }
        if (intersects(requestedActions, properties.actions().persist())) {
            return explicit(NlpOperationFrame.Effect.PERSIST);
        }
        if (intersects(requestedActions, properties.actions().construct())) {
            if (persistentTarget) {
                return explicit(NlpOperationFrame.Effect.PERSIST);
            }
            if (presentationTarget) {
                return explicit(NlpOperationFrame.Effect.PRESENT);
            }
            return unknown();
        }
        if (intersects(requestedActions, properties.actions().presentation())) {
            return explicit(NlpOperationFrame.Effect.PRESENT);
        }
        if (intersects(requestedActions, properties.actions().read()) || question) {
            return explicit(NlpOperationFrame.Effect.READ);
        }
        if (!requestedActions.isEmpty() && persistentTarget) {
            return new Classification(
                    NlpOperationFrame.Effect.PERSIST,
                    NlpOperationFrame.Certainty.INFERRED
            );
        }
        if (!requestedActions.isEmpty() && domainDataTarget) {
            return new Classification(
                    NlpOperationFrame.Effect.UNRESOLVED_ACTION,
                    NlpOperationFrame.Certainty.INFERRED
            );
        }
        return unknown();
    }

    private boolean canRequestOperation(
            NlpAnalysis.Token token,
            Set<Integer> rootIndexes,
            Set<Integer> inferredActionIndexes
    ) {
        return "VB".equals(token.partOfSpeech())
                || "VBP".equals(token.partOfSpeech())
                || rootIndexes.contains(token.index()) && !isVerb(token)
                || inferredActionIndexes.contains(token.index());
    }

    private boolean isVerb(NlpAnalysis.Token token) {
        return token.partOfSpeech() != null && token.partOfSpeech().startsWith("VB");
    }

    private boolean isKnownAction(String value) {
        return contains(properties.actions().read(), normalize(value))
                || contains(properties.actions().presentation(), normalize(value))
                || contains(properties.actions().construct(), normalize(value))
                || contains(properties.actions().persist(), normalize(value))
                || contains(properties.actions().modify(), normalize(value))
                || contains(properties.actions().transfer(), normalize(value))
                || contains(properties.actions().external(), normalize(value));
    }

    private boolean isObjectRelation(String relation) {
        return "obj".equals(relation) || "iobj".equals(relation);
    }

    private NlpAnalysis.Token token(List<NlpAnalysis.Token> tokens, int index) {
        return tokens.stream()
                .filter(token -> token.index() == index)
                .findFirst()
                .orElse(null);
    }

    private boolean isKnownTarget(String value) {
        return contains(properties.targets().presentation(), value)
                || contains(properties.targets().persistent(), value)
                || contains(properties.targets().domainData(), value);
    }

    private boolean intersects(Iterable<String> left, List<String> right) {
        for (String value : left) {
            if (contains(right, value)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(List<String> values, String expected) {
        return values.stream().map(this::normalize).anyMatch(expected::equals);
    }

    private List<String> distinct(List<String> values) {
        Set<String> distinct = new LinkedHashSet<>();
        values.stream().map(this::normalize).forEach(distinct::add);
        return List.copyOf(distinct);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private Classification explicit(NlpOperationFrame.Effect effect) {
        return new Classification(effect, NlpOperationFrame.Certainty.EXPLICIT);
    }

    private Classification unknown() {
        return new Classification(
                NlpOperationFrame.Effect.UNKNOWN,
                NlpOperationFrame.Certainty.UNKNOWN
        );
    }

    private record Classification(
            NlpOperationFrame.Effect effect,
            NlpOperationFrame.Certainty certainty
    ) { }
}
