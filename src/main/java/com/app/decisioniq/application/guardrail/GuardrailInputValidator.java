package com.app.decisioniq.application.guardrail;

import org.springframework.stereotype.Component;

@Component
public class GuardrailInputValidator {

    /**
     * Returns whether a question has usable content and contains no unsafe control characters.
     */
    public boolean isValid(String originalQuestion, String normalizedQuestion) {
        return originalQuestion != null
                && !normalizedQuestion.isBlank()
                && !containsInvalidControlCharacter(originalQuestion);
    }

    /**
     * Detects non-whitespace ISO control characters that should not cross the request boundary.
     */
    private boolean containsInvalidControlCharacter(String question) {
        return question.codePoints()
                .anyMatch(codePoint -> Character.isISOControl(codePoint)
                        && !Character.isWhitespace(codePoint));
    }
}
