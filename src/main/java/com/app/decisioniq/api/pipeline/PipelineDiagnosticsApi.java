package com.app.decisioniq.api.pipeline;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import com.app.decisioniq.api.filter.RequestIdentityFilter;
import com.app.decisioniq.application.assistant.AssistantRequestCommand;
import com.app.decisioniq.application.assistant.AssistantRequestUseCase;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!prod")
@RequestMapping("/internal/dev/pipeline")
public class PipelineDiagnosticsApi {

    private final AssistantRequestUseCase requestUseCase;
    private final PipelineDiagnosticsMapper responseMapper;

    public PipelineDiagnosticsApi(
            AssistantRequestUseCase requestUseCase,
            PipelineDiagnosticsMapper responseMapper
    ) {
        this.requestUseCase = requestUseCase;
        this.responseMapper = responseMapper;
    }

    @PostMapping(
            value = "/evaluate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public PipelineEvaluationResponse evaluate(
            @Valid @RequestBody AssistantAskRequest request,
            @RequestAttribute(RequestIdentityFilter.CORRELATION_ATTRIBUTE) String correlationId,
            @RequestAttribute(RequestIdentityFilter.REQUEST_ATTRIBUTE) String requestId
    ) {
        return responseMapper.toResponse(requestUseCase.handle(new AssistantRequestCommand(
                request.tenantId(),
                request.userId(),
                request.designation(),
                request.conversationId(),
                request.question(),
                correlationId,
                requestId
        )));
    }
}
