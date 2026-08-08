package com.app.decisioniq.application.nlp;

import com.app.decisioniq.config.nlp.OperationPolicyProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Separates same-message factual context from executable read requests before catalog retrieval.
 * This is intentionally narrow: an unrecognised declarative clause remains executable instead of
 * being silently discarded.
 */
@Service
public class NlpClauseRoleAnalyzer {

    private static final List<String> QUESTION_WORDS = List.of(
            "what", "why", "which", "when", "where", "who", "how", "can", "could", "would", "was", "were", "is", "are", "do", "does", "did"
    );
    private final OperationPolicyProperties operationPolicy;

    public NlpClauseRoleAnalyzer(OperationPolicyProperties operationPolicy) {
        this.operationPolicy = operationPolicy;
    }

    public List<NlpOperationFrame> classify(NlpAnalysis analysis, List<NlpOperationFrame> frames) {
        List<NlpOperationFrame> classified = new ArrayList<>(frames.size());
        String activeTransactionId = null;
        for (NlpOperationFrame frame : frames) {
            Optional<String> explicitId = transactionId(frame.text());
            NlpClauseRole role = roleFor(analysis, frame, explicitId);
            NlpContextCandidate candidate = role == NlpClauseRole.CONTEXT_STATEMENT
                    ? new NlpContextCandidate(explicitId.orElseThrow(), assertedOutcome(frame.text()))
                    : null;
            String resolvedTransactionId = explicitId.orElse(activeTransactionId);
            NlpOperationFrame resolved = frame.withRole(role, candidate, resolvedTransactionId);
            classified.add(resolved);
            if (candidate != null) {
                activeTransactionId = candidate.transactionId();
            } else if (role == NlpClauseRole.EXECUTABLE_REQUEST && explicitId.isPresent()) {
                // A later "it/this transaction" clause may refer to this explicit lookup.
                activeTransactionId = explicitId.get();
            }
        }
        return List.copyOf(classified);
    }

    private NlpClauseRole roleFor(
            NlpAnalysis analysis,
            NlpOperationFrame frame,
            Optional<String> explicitId
    ) {
        if (isIgnoredHarmlessNoise(frame.text(), explicitId, analysis, frame)) {
            return NlpClauseRole.IGNORED_HARMLESS_NOISE;
        }
        return isContextStatement(analysis, frame, explicitId)
                ? NlpClauseRole.CONTEXT_STATEMENT
                : NlpClauseRole.EXECUTABLE_REQUEST;
    }

    private boolean isIgnoredHarmlessNoise(
            String text,
            Optional<String> explicitId,
            NlpAnalysis analysis,
            NlpOperationFrame frame
    ) {
        if (explicitId.isPresent() || isQuestion(text) || isImperative(analysis, frame)) {
            return false;
        }
        String normalized = normalize(text);
        return operationPolicy.routing().ignoredHarmlessPhrases().stream()
                .map(this::normalize)
                .anyMatch(normalized::equals);
    }

    private boolean isContextStatement(
            NlpAnalysis analysis,
            NlpOperationFrame frame,
            Optional<String> explicitId
    ) {
        if (explicitId.isEmpty() || isQuestion(frame.text()) || isImperative(analysis, frame)) {
            return false;
        }
        return true;
    }

    private boolean isQuestion(String text) {
        String normalized = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("?")) {
            return true;
        }
        String first = normalized.replaceFirst("^[^a-z0-9]+", "").split("\\s+", 2)[0];
        return QUESTION_WORDS.contains(first);
    }

    private boolean isImperative(NlpAnalysis analysis, NlpOperationFrame frame) {
        String[] words = frame.text().toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        int firstContentIndex = words.length > 0 && "please".equals(words[0]) ? 1 : 0;
        String firstWord = firstContentIndex < words.length ? words[firstContentIndex] : "";
        if (isRequestAction(firstWord)) {
            return true;
        }
        return sentenceFor(analysis, frame).map(sentence -> sentence.tokens().stream()
                .filter(token -> token.index() >= firstToken(frame, sentence))
                .filter(token -> token.index() <= lastToken(frame, sentence))
                .filter(token -> token.partOfSpeech() != null && !token.partOfSpeech().matches("[.,:;]+"))
                .findFirst()
                .map(token -> "VB".equals(token.partOfSpeech())
                        && isRequestAction(token.lemma()))
                .orElse(false)).orElse(false);
    }

    private Optional<NlpAnalysis.Sentence> sentenceFor(NlpAnalysis analysis, NlpOperationFrame frame) {
        return analysis.sentences().stream().filter(sentence -> sentence.index() == frame.sentenceIndex()).findFirst();
    }

    private int firstToken(NlpOperationFrame frame, NlpAnalysis.Sentence sentence) {
        return sentence.clauses().stream().filter(clause -> clause.index() == frame.clauseIndex())
                .findFirst().map(NlpAnalysis.Clause::firstTokenIndex).orElse(Integer.MIN_VALUE);
    }

    private int lastToken(NlpOperationFrame frame, NlpAnalysis.Sentence sentence) {
        return sentence.clauses().stream().filter(clause -> clause.index() == frame.clauseIndex())
                .findFirst().map(NlpAnalysis.Clause::lastTokenIndex).orElse(Integer.MAX_VALUE);
    }

    private Optional<String> transactionId(String text) {
        return NlpTransactionReferenceExtractor.first(text);
    }

    private boolean isReadAction(String value) {
        return operationPolicy.routing().readActions().stream()
                .anyMatch(action -> action.equalsIgnoreCase(value));
    }

    private boolean isRequestAction(String value) {
        return isReadAction(value) || operationPolicy.actions().construct().stream()
                .anyMatch(action -> action.equalsIgnoreCase(value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String assertedOutcome(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("declined") || normalized.contains("rejected")) {
            return "DECLINED";
        }
        if (normalized.contains("approved") || normalized.contains("accepted") || normalized.contains("let through")) {
            return "APPROVED";
        }
        return null;
    }
}
