package com.app.decisioniq.application.guardrail;

import com.app.decisioniq.config.guardrail.GuardrailProperties;
import org.springframework.stereotype.Component;

@Component
public class GuardrailTextNormalizer {

    private final GuardrailProperties properties;

    /**
     * Creates a normalizer backed by the versioned guardrail normalization policy.
     */
    public GuardrailTextNormalizer(GuardrailProperties properties) {
        this.properties = properties;
    }

    /**
     * Removes harmless formatting variance while preserving identifiers, values, and words.
     */
    public String normalizeQuestion(String question) {
        if (question == null) {
            return "";
        }

        String normalized = question.strip();
        if (properties.normalization().collapseWhitespace()) {
            normalized = collapseWhitespace(normalized);
        }
        normalized = collapseRepeatedCharacter(
                normalized,
                '!',
                properties.normalization().repeatedPunctuationLimit()
        );
        normalized = collapseRepeatedCharacter(
                normalized,
                '?',
                properties.normalization().repeatedPunctuationLimit()
        );
        return collapseRepeatedCharacter(
                normalized,
                '.',
                properties.normalization().ellipsisLimit()
        );
    }

    /**
     * Replaces each consecutive whitespace run with one ordinary space.
     */
    private String collapseWhitespace(String value) {
        StringBuilder normalized = new StringBuilder(value.length());
        boolean previousWhitespace = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character)) {
                if (!previousWhitespace) {
                    normalized.append(' ');
                }
                previousWhitespace = true;
            } else {
                normalized.append(character);
                previousWhitespace = false;
            }
        }
        return normalized.toString();
    }

    /**
     * Limits repeated punctuation without changing any other characters in the question.
     */
    private String collapseRepeatedCharacter(String value, char target, int limit) {
        StringBuilder normalized = new StringBuilder(value.length());
        int runLength = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == target) {
                runLength++;
                if (runLength <= limit) {
                    normalized.append(character);
                }
            } else {
                runLength = 0;
                normalized.append(character);
            }
        }
        return normalized.toString();
    }
}
