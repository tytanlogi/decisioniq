package com.app.decisioniq.application.guardrail;

import org.springframework.stereotype.Component;

@Component
public class GuardrailInputValidator {

    public boolean isInvalid(String originalQuestion, String normalizedQuestion) {
        return originalQuestion == null
                || normalizedQuestion.isBlank()
                || containsInvalidControlCharacter(originalQuestion);
    }

    private boolean containsInvalidControlCharacter(String question) {
        return question.codePoints()
                .anyMatch(codePoint -> Character.isISOControl(codePoint)
                        && !Character.isWhitespace(codePoint));
    }
}
