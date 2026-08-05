package com.app.decisioniq.api.pipeline;

import com.app.decisioniq.application.assistant.AssistantRequestResult;
import com.app.decisioniq.application.catalog.CatalogFrameValidation.UnitValidation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PipelineDiagnosticsMapper {

    public PipelineEvaluationResponse toResponse(AssistantRequestResult result) {
        List<PipelineEvaluationResponse.RejectedPart> rejectedParts =
                result.catalogValidation() == null
                        ? List.of()
                        : result.catalogValidation().units().stream()
                                .filter(unit -> !unit.includedInEffectiveQuestion())
                                .map(this::toRejectedPart)
                                .toList();

        return new PipelineEvaluationResponse(result.effectiveQuestion(), rejectedParts);
    }

    private PipelineEvaluationResponse.RejectedPart toRejectedPart(UnitValidation unit) {
        return new PipelineEvaluationResponse.RejectedPart(
                unit.frame().text(),
                unit.frame().effect().name(),
                unit.catalogOutcome().name()
        );
    }
}
