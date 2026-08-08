package com.app.decisioniq.infrastructure.llm.openai;

import com.app.decisioniq.application.assistant.InterpretationInput;
import com.app.decisioniq.application.catalog.SupportedCatalogUnit;
import com.app.decisioniq.application.catalog.UnsupportedCatalogUnit;
import com.app.decisioniq.application.interpretation.InterpretationValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class OpenAiPromptFactory {

    static final String INSTRUCTIONS = """
            Classify every ordered DecisionIQ unit exactly once. Use only these operations:
            EXPLAIN, LOOKUP, LIST, COUNT, COMPARE, SUMMARIZE, NONE. SUPPORTED_REQUEST requires one
            or more supplied candidate keys. Never invent keys, identifiers, fields or values.
            Use contextSourceUnitId only to reference an earlier unit; otherwise use NONE.
            IGNORE_NOISE and OUT_OF_SCOPE require operation NONE, no keys and context NONE.
            Use CLARIFY only when user input is necessary. Overall disposition is
            READY_FOR_PLANNING when supported units need no clarification,
            CLARIFICATION_REQUIRED when clarification is required, otherwise OUT_OF_SCOPE.
            """;

    private final ObjectMapper objectMapper;

    public OpenAiPromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String create(InterpretationInput input) {
        List<PromptUnit> units = Stream.concat(
                        input.supportedUnits().stream().map(this::supportedUnit),
                        input.unsupportedUnits().stream().map(this::unsupportedUnit)
                )
                .sorted(Comparator.comparingInt(OrderedPromptUnit::sentenceIndex)
                        .thenComparingInt(OrderedPromptUnit::clauseIndex))
                .map(OrderedPromptUnit::unit)
                .toList();
        PromptInput promptInput = new PromptInput(input.schemaVersion(), units);
        try {
            return objectMapper.writeValueAsString(promptInput);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize interpretation input", exception);
        }
    }

    private OrderedPromptUnit supportedUnit(SupportedCatalogUnit supported) {
        return new OrderedPromptUnit(
                supported.unit().sentenceIndex(),
                supported.unit().clauseIndex(),
                new PromptUnit(
                        InterpretationValidator.unitId(
                                supported.unit().sentenceIndex(), supported.unit().clauseIndex()
                        ),
                        supported.unit().text(),
                        supported.candidates().stream()
                                .map(candidate -> candidate.catalogKey())
                                .toList()
                )
        );
    }

    private OrderedPromptUnit unsupportedUnit(UnsupportedCatalogUnit unsupported) {
        return new OrderedPromptUnit(
                unsupported.unit().sentenceIndex(),
                unsupported.unit().clauseIndex(),
                new PromptUnit(
                        InterpretationValidator.unitId(
                                unsupported.unit().sentenceIndex(), unsupported.unit().clauseIndex()
                        ),
                        unsupported.unit().text(),
                        List.of()
                )
        );
    }

    private record PromptInput(
            String schemaVersion,
            List<PromptUnit> units
    ) { }

    private record PromptUnit(
            String unitId,
            String text,
            List<String> candidateKeys
    ) { }

    private record OrderedPromptUnit(
            int sentenceIndex,
            int clauseIndex,
            PromptUnit unit
    ) { }
}
