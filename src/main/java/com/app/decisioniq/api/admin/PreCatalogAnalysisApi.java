package com.app.decisioniq.api.admin;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import com.app.decisioniq.api.filter.RequestIdentityFilter;
import com.app.decisioniq.application.assistant.AssistantRequestCommand;
import com.app.decisioniq.application.assistant.PreCatalogAnalysisResult;
import com.app.decisioniq.application.assistant.PreCatalogAnalysisService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Non-production diagnostic endpoint. It exposes the logical pre-catalog payload but never calls
 * the remote catalog service, Milvus, OpenAI, or evidence services.
 */
@RestController
@Profile("!prod")
@RequestMapping("/api/admin/pre-catalog-analysis")
public class PreCatalogAnalysisApi {

    private final PreCatalogAnalysisService analysisService;

    public PreCatalogAnalysisApi(PreCatalogAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PreCatalogAnalysisResult analyze(
            @Valid @RequestBody AssistantAskRequest request,
            @RequestAttribute(RequestIdentityFilter.CORRELATION_ATTRIBUTE) String correlationId,
            @RequestAttribute(RequestIdentityFilter.REQUEST_ATTRIBUTE) String requestId
    ) {
        return analysisService.analyze(new AssistantRequestCommand(
                request.tenantId(),
                request.userId(),
                request.designation(),
                request.conversationId(),
                request.question(),
                correlationId,
                requestId
        ));
    }
}
