package com.app.decisioniq.application.guardrail;

import org.springframework.stereotype.Component;

@Component
public class GuardrailInputValidator {

    public boolean isInvalid(String originalQuestion, String normalizedQuestion) {
        return originalQuestion == null
                || normalizedQuestion.isBlank()
                || containsInvalidControlCharacter(originalQuestion)
                || hasUnbalancedDelimiters(normalizedQuestion);
    }

    private boolean containsInvalidControlCharacter(String question) {
        return question.codePoints()
                .anyMatch(codePoint -> Character.isISOControl(codePoint)
                        && !Character.isWhitespace(codePoint));
    }

    private boolean hasUnbalancedDelimiters(String value) {
        return count(value, '"') % 2 != 0
                || !balanced(value, '(', ')')
                || !balanced(value, '[', ']')
                || !balanced(value, '{', '}');
    }

    private long count(String value, char target) {
        return value.chars().filter(character -> character == target).count();
    }

    private boolean balanced(String value, char open, char close) {
        int depth = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == open) {
                depth++;
            } else if (current == close && --depth < 0) {
                return false;
            }
        }
        return depth == 0;
    }
}
