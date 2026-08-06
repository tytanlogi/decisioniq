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
        Set<Integer> auxiliaryIndexes = dependencies.stream()
                .filter(dependency -> isAuxiliaryRelation(dependency.relation()))
                .map(NlpAnalysis.Dependency::dependentIndex)
                .collect(Collectors.toSet());
        Set<Integer> inferredActionIndexes = new HashSet<>();
        List<NlpAnalysis.Token> actionTokens = new ArrayList<>(tokens.stream()
                .filter(token -> isVerb(token) && !auxiliaryIndexes.contains(token.index())
                        || rootIndexes.contains(token.index()) && isKnownAction(token.lemma()))
                .toList());
        NlpAnalysis.Token leadingToken = firstContentToken(tokens);
        if (leadingToken != null
                && isKnownAction(leadingToken.lemma())
                && actionTokens.stream().noneMatch(token -> token.index() == leadingToken.index())) {
            actionTokens.add(leadingToken);
            inferredActionIndexes.add(leadingToken.index());
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

        NlpOperationFrame.Effect effect = classify(
                dependencies, actionTokens, rootIndexes, auxiliaryIndexes,
                inferredActionIndexes, targets
        );
        return new NlpOperationFrame(
                sentence.index(),
                clause.index(),
                clause.text(),
                actions,
                objects,
                targets,
                effect
        );
    }

    private NlpOperationFrame.Effect classify(
            List<NlpAnalysis.Dependency> dependencies,
            List<NlpAnalysis.Token> actionTokens,
            Set<Integer> rootIndexes,
            Set<Integer> auxiliaryIndexes,
            Set<Integer> inferredActionIndexes,
            List<String> targets
    ) {
        Set<String> requestedActions = actionTokens.stream()
                .filter(token -> canRequestOperation(
                        token, actionTokens, dependencies, rootIndexes, auxiliaryIndexes,
                        inferredActionIndexes
                ))
                .map(NlpAnalysis.Token::lemma)
                .map(this::normalize)
                .collect(Collectors.toSet());
        boolean persistentTarget = intersects(targets, properties.targets().persistent());

        if (intersects(requestedActions, properties.actions().modify())) {
            return NlpOperationFrame.Effect.MODIFY;
        }
        if (intersects(requestedActions, properties.actions().external())) {
            return NlpOperationFrame.Effect.EXTERNAL_ACTION;
        }
        if (intersects(requestedActions, properties.actions().transfer())) {
            return NlpOperationFrame.Effect.TRANSFER;
        }
        if (intersects(requestedActions, properties.actions().persist())) {
            return NlpOperationFrame.Effect.PERSIST;
        }
        if (intersects(requestedActions, properties.actions().construct())) {
            if (persistentTarget) {
                return NlpOperationFrame.Effect.PERSIST;
            }
        }
        return NlpOperationFrame.Effect.SAFE_CANDIDATE;
    }

    private boolean canRequestOperation(
            NlpAnalysis.Token token,
            List<NlpAnalysis.Token> actionTokens,
            List<NlpAnalysis.Dependency> dependencies,
            Set<Integer> rootIndexes,
            Set<Integer> auxiliaryIndexes,
            Set<Integer> inferredActionIndexes
    ) {
        if (inferredActionIndexes.contains(token.index())) {
            return true;
        }
        if (auxiliaryIndexes.contains(token.index())) {
            return false;
        }
        return "VB".equals(token.partOfSpeech())
                || "VBP".equals(token.partOfSpeech())
                || isRequestedComplement(token, dependencies)
                || isGerundAfterImperativeRoot(token, actionTokens, rootIndexes)
                || rootIndexes.contains(token.index()) && !isVerb(token);
    }

    private boolean isGerundAfterImperativeRoot(
            NlpAnalysis.Token token,
            List<NlpAnalysis.Token> actionTokens,
            Set<Integer> rootIndexes
    ) {
        if (!"VBG".equals(token.partOfSpeech())) {
            return false;
        }
        return actionTokens.stream().anyMatch(candidate ->
                candidate.index() < token.index()
                        && rootIndexes.contains(candidate.index())
                        && "VB".equals(candidate.partOfSpeech())
        );
    }

    private boolean isRequestedComplement(
            NlpAnalysis.Token token,
            List<NlpAnalysis.Dependency> dependencies
    ) {
        return dependencies.stream().anyMatch(dependency ->
                dependency.dependentIndex() == token.index()
                        && ("xcomp".equals(dependency.relation())
                        || "ccomp".equals(dependency.relation()))
        );
    }

    private NlpAnalysis.Token firstContentToken(List<NlpAnalysis.Token> tokens) {
        return tokens.stream()
                .filter(token -> token.partOfSpeech() == null
                        || !token.partOfSpeech().matches("[.,:;]+"))
                .findFirst()
                .orElse(null);
    }

    private boolean isAuxiliaryRelation(String relation) {
        return relation != null
                && (relation.equals("cop")
                || relation.equals("aux")
                || relation.startsWith("aux:"));
    }

    private boolean isVerb(NlpAnalysis.Token token) {
        return token.partOfSpeech() != null && token.partOfSpeech().startsWith("VB");
    }

    private boolean isKnownAction(String value) {
        return contains(properties.actions().construct(), normalize(value))
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
                || contains(properties.targets().persistent(), value);
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

}
