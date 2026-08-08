package com.app.decisioniq.infrastructure.llm.openai;

import com.app.decisioniq.application.interpretation.QueryInterpretation;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExternalContentMasker {

    private static final Pattern BUSINESS_IDENTIFIER = Pattern.compile(
            "(?i)\\b(?:TXN|CASE|CORR|CUST|ACC)[-_:][A-Z0-9-]+\\b"
    );
    private static final Pattern UUID = Pattern.compile(
            "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\b"
    );

    public MaskedContent mask(String content) {
        Map<String, String> replacements = new LinkedHashMap<>();
        AtomicInteger sequence = new AtomicInteger(1);
        String masked = replace(content, BUSINESS_IDENTIFIER, replacements, sequence);
        masked = replace(masked, UUID, replacements, sequence);
        return new MaskedContent(masked, Map.copyOf(replacements));
    }

    private String replace(
            String content,
            Pattern pattern,
            Map<String, String> replacements,
            AtomicInteger sequence
    ) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String placeholder = "DIQ_ID_" + sequence.getAndIncrement();
            replacements.put(placeholder, matcher.group());
            matcher.appendReplacement(output, placeholder);
        }
        matcher.appendTail(output);
        return output.toString();
    }

    public record MaskedContent(String value, Map<String, String> replacements) {

        public QueryInterpretation restore(QueryInterpretation interpretation) {
            return new QueryInterpretation(
                    interpretation.schemaVersion(),
                    interpretation.disposition(),
                    interpretation.units(),
                    interpretation.clarificationQuestions().stream()
                            .map(this::restoreText)
                            .toList(),
                    interpretation.model(),
                    interpretation.promptVersion(),
                    interpretation.providerResponseId()
            );
        }

        private String restoreText(String value) {
            String restored = value;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                restored = restored.replace(entry.getKey(), entry.getValue());
            }
            return restored;
        }
    }
}
