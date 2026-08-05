package com.app.decisioniq.api.pipeline;

import java.util.List;

public record PipelineEvaluationResponse(
        String questionForLlm,
        List<RejectedPart> rejectedParts
) {
    public PipelineEvaluationResponse {
        rejectedParts = List.copyOf(rejectedParts);
    }

    public record RejectedPart(
            String text,
            String classification,
            String reason
    ) {
    }
}
