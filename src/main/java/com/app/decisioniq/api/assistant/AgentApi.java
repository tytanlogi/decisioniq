package com.app.decisioniq.api.assistant;

import com.app.decisioniq.api.assistant.mapper.AssistantGuardrailHttpMapper;
import com.app.decisioniq.api.assistant.model.AssistantAnswerResponse;
import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import com.app.decisioniq.api.filter.RequestIdentityFilter;
import com.app.decisioniq.application.assistant.AssistantRequestCommand;
import com.app.decisioniq.application.assistant.AssistantRequestUseCase;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping({"/agent", "/api/assistant"})
public class AgentApi {

    private final AssistantRequestUseCase requestUseCase;
    private final AssistantGuardrailHttpMapper responseMapper;

    public AgentApi(
            AssistantRequestUseCase requestUseCase,
            AssistantGuardrailHttpMapper responseMapper
    ) {
        this.requestUseCase = requestUseCase;
        this.responseMapper = responseMapper;
    }

    @PostMapping(value = "/ask",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AssistantAnswerResponse ask(@Valid @RequestBody AssistantAskRequest request,
            @RequestAttribute(RequestIdentityFilter.CORRELATION_ATTRIBUTE) String correlationId,
            @RequestAttribute(RequestIdentityFilter.REQUEST_ATTRIBUTE) String requestId) {
        AssistantRequestCommand command = new AssistantRequestCommand(
                request.tenantId(),
                request.userId(),
                request.designation(),
                request.conversationId(),
                request.question(),
                correlationId,
                requestId
        );
        return responseMapper.toResponse(requestUseCase.handle(command));
    }
}
