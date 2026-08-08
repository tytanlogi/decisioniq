package com.app.decisioniq.application.interpretation;

import com.app.decisioniq.application.assistant.InterpretationInput;
import com.app.decisioniq.application.catalog.SupportedCatalogUnit;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class InterpretationValidator {

    public void validate(InterpretationInput input, QueryInterpretation interpretation) {
        if (interpretation == null || interpretation.disposition() == null) {
            throw invalid("The model did not return a disposition");
        }

        Map<String, Set<String>> allowedCatalogKeys = supportedCatalogKeys(input);
        List<String> orderedUnitIds = orderedUnitIds(input);
        Set<String> knownUnitIds = Set.copyOf(orderedUnitIds);
        Set<String> interpretedUnitIds = new HashSet<>();
        boolean hasSupportedRequest = false;
        boolean hasClarification = false;

        for (int index = 0; index < interpretation.units().size(); index++) {
            InterpretedUnit unit = interpretation.units().get(index);
            if (unit == null || unit.sourceUnitId() == null || unit.disposition() == null) {
                throw invalid("The model returned an incomplete unit");
            }
            if (!knownUnitIds.contains(unit.sourceUnitId())) {
                throw invalid("The model referenced an unknown source unit");
            }
            if (!interpretedUnitIds.add(unit.sourceUnitId())) {
                throw invalid("The model returned a source unit more than once");
            }
            if (index >= orderedUnitIds.size()
                    || !orderedUnitIds.get(index).equals(unit.sourceUnitId())) {
                throw invalid("The model changed source unit order");
            }

            if (unit.disposition() == UnitDisposition.SUPPORTED_REQUEST) {
                validateSupportedUnit(unit, allowedCatalogKeys);
                validateContextReference(unit, orderedUnitIds, index);
                hasSupportedRequest = true;
            } else {
                validateNonSupportedUnit(unit);
            }
            hasClarification |= unit.disposition() == UnitDisposition.CLARIFY;
        }

        if (!interpretedUnitIds.equals(knownUnitIds)) {
            throw invalid("The model did not assess every source unit");
        }
        if (interpretation.disposition() == InterpretationDisposition.READY_FOR_PLANNING
                && (!hasSupportedRequest || hasClarification)) {
            throw invalid("Planning disposition is inconsistent with the assessed units");
        }
        if (interpretation.disposition() == InterpretationDisposition.CLARIFICATION_REQUIRED
                && (!hasClarification || interpretation.clarificationQuestions().isEmpty())) {
            throw invalid("Clarification disposition is incomplete");
        }
        if (interpretation.disposition() == InterpretationDisposition.OUT_OF_SCOPE
                && (hasSupportedRequest || hasClarification)) {
            throw invalid("Out-of-scope disposition is inconsistent with the assessed units");
        }
    }

    private void validateSupportedUnit(
            InterpretedUnit unit,
            Map<String, Set<String>> allowedCatalogKeys
    ) {
        Set<String> allowed = allowedCatalogKeys.get(unit.sourceUnitId());
        if (allowed == null) {
            throw invalid("An unsupported source unit was promoted without catalog authority");
        }
        if (unit.selectedCatalogKeys().isEmpty()) {
            throw invalid("A supported unit did not select a catalog capability");
        }
        if (unit.operation() == null || unit.operation() == InterpretationOperation.NONE) {
            throw invalid("A supported unit did not provide an operation");
        }
        if (!allowed.containsAll(unit.selectedCatalogKeys())) {
            throw invalid("The model selected a catalog capability that was not retrieved");
        }
    }

    private void validateContextReference(
            InterpretedUnit unit,
            List<String> orderedUnitIds,
            int unitIndex
    ) {
        String contextSource = unit.contextSourceUnitId();
        if (contextSource == null || contextSource.isBlank() || "NONE".equals(contextSource)) {
            return;
        }
        int contextIndex = orderedUnitIds.indexOf(contextSource);
        if (contextIndex < 0 || contextIndex >= unitIndex) {
            throw invalid("A context reference must target an earlier source unit");
        }
    }

    private void validateNonSupportedUnit(InterpretedUnit unit) {
        if (!unit.selectedCatalogKeys().isEmpty()) {
            throw invalid("A non-supported unit selected catalog capabilities");
        }
        if (unit.operation() != InterpretationOperation.NONE) {
            throw invalid("A non-supported unit selected an operation");
        }
        if (unit.contextSourceUnitId() != null
                && !unit.contextSourceUnitId().isBlank()
                && !"NONE".equals(unit.contextSourceUnitId())) {
            throw invalid("A non-supported unit selected context");
        }
    }

    private Map<String, Set<String>> supportedCatalogKeys(InterpretationInput input) {
        Map<String, Set<String>> allowed = new HashMap<>();
        for (SupportedCatalogUnit supported : input.supportedUnits()) {
            allowed.put(
                    unitId(supported.unit().sentenceIndex(), supported.unit().clauseIndex()),
                    supported.candidates().stream()
                            .map(candidate -> candidate.catalogKey())
                            .collect(java.util.stream.Collectors.toUnmodifiableSet())
            );
        }
        return allowed;
    }

    private List<String> orderedUnitIds(InterpretationInput input) {
        return java.util.stream.Stream.concat(
                        input.supportedUnits().stream().map(supported -> supported.unit()),
                        input.unsupportedUnits().stream().map(unsupported -> unsupported.unit())
                )
                .sorted(java.util.Comparator.comparingInt(
                                com.app.decisioniq.application.nlp.NlpOperationFrame::sentenceIndex
                        )
                        .thenComparingInt(
                                com.app.decisioniq.application.nlp.NlpOperationFrame::clauseIndex
                        ))
                .map(unit -> unitId(unit.sentenceIndex(), unit.clauseIndex()))
                .toList();
    }

    public static String unitId(int sentenceIndex, int clauseIndex) {
        return "s" + sentenceIndex + "c" + clauseIndex;
    }

    private InvalidInterpretationException invalid(String message) {
        return new InvalidInterpretationException(message);
    }
}
