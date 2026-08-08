package com.app.decisioniq.application.nlp;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Recognises a narrow, explainable comparison shape before semantic retrieval. It does not infer
 * values or answers: it only prevents related lookups from being sent as unrelated searches.
 */
@Service
public class NlpRequestGroupAnalyzer {

    private static final Pattern COMPARISON_LANGUAGE = Pattern.compile(
            "(?i)\\b(compare|comparison|versus|vs\\.?|same|different|difference|even\\s+though|than)\\b"
    );
    private static final Pattern UNRESOLVED_COMPARISON_REFERENCE = Pattern.compile(
            "(?i)\\b(other|another|them|that\\s+(?:one|transaction)|second\\s+transaction)\\b"
    );

    public List<NlpRequestGroup> group(String normalizedQuestion, List<NlpOperationFrame> frames) {
        List<Integer> executableIndexes = IntStream.range(0, frames.size())
                .filter(index -> frames.get(index).executable())
                .boxed()
                .toList();
        long distinctTransactionIds = executableIndexes.stream()
                .flatMap(index -> NlpTransactionReferenceExtractor.all(frames.get(index).text()).stream())
                .distinct()
                .count();
        boolean comparisonLanguage = COMPARISON_LANGUAGE.matcher(normalizedQuestion).find();
        if (distinctTransactionIds >= 2 && comparisonLanguage) {
            return List.of(new NlpRequestGroup(
                    NlpRequestGroup.Kind.COMPARISON,
                    executableIndexes,
                    comparisonAspect(normalizedQuestion)
            ));
        }
        if (distinctTransactionIds == 1
                && comparisonLanguage
                && UNRESOLVED_COMPARISON_REFERENCE.matcher(normalizedQuestion).find()) {
            return List.of(new NlpRequestGroup(
                    NlpRequestGroup.Kind.CLARIFY,
                    executableIndexes,
                    "MISSING_COMPARISON_REFERENCE"
            ));
        }
        return executableIndexes.stream()
                .map(index -> new NlpRequestGroup(NlpRequestGroup.Kind.LOOKUP, List.of(index), null))
                .toList();
    }

    private String comparisonAspect(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("model score") || normalized.contains("score")) {
            return "MODEL_SCORE";
        }
        if (normalized.contains("risk band") || normalized.contains("risk")) {
            return "RISK";
        }
        if (normalized.contains("rule")) {
            return "RULE";
        }
        if (normalized.contains("approv") || normalized.contains("declin") || normalized.contains("decision")) {
            return "DECISION";
        }
        return "UNSPECIFIED";
    }
}
