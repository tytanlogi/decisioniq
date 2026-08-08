package com.app.decisioniq.application.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts exact local transaction-like references without correcting or guessing identifiers.
 */
public final class NlpTransactionReferenceExtractor {

    private static final Pattern TRANSACTION_ID = Pattern.compile(
            "(?i)\\b((?:txn|tx|case|corr(?:elation)?)-[0-9][a-z0-9-]*)\\b|"
                    + "\\btransaction\\s+((?:(?:txn|tx)-)?[0-9][a-z0-9-]*)\\b"
    );

    private NlpTransactionReferenceExtractor() {
    }

    public static Optional<String> first(String text) {
        return all(text).stream().findFirst();
    }

    public static List<String> all(String text) {
        Matcher matcher = TRANSACTION_ID.matcher(text == null ? "" : text);
        List<String> identifiers = new ArrayList<>();
        while (matcher.find()) {
            String identifier = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String normalized = identifier.toUpperCase(Locale.ROOT);
            if (!identifiers.contains(normalized)) {
                identifiers.add(normalized);
            }
        }
        return List.copyOf(identifiers);
    }
}
