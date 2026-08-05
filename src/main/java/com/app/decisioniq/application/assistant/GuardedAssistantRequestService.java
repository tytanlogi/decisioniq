package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.catalog.CatalogFrameValidation;
import com.app.decisioniq.application.catalog.CatalogRelevanceService;
import com.app.decisioniq.application.guardrail.ConfiguredRequestBoundaryValidator;
import com.app.decisioniq.application.guardrail.RequestGuardrailService;
import com.app.decisioniq.application.nlp.NlpAnalysis;
import com.app.decisioniq.application.nlp.NlpAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision;
import com.app.decisioniq.domain.context.TrustedRequestContext;
import com.app.decisioniq.domain.context.TrustedRequestContextResolver;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuardedAssistantRequestService implements AssistantRequestUseCase {

    private static final Logger log = LoggerFactory.getLogger(GuardedAssistantRequestService.class);

    private final ConfiguredRequestBoundaryValidator boundaryValidator;
    private final TrustedRequestContextResolver contextResolver;
    private final RequestGuardrailService guardrailService;
    private final NlpAnalyzer nlpAnalyzer;
    private final NlpOperationAnalyzer operationAnalyzer;
    private final CatalogRelevanceService catalogRelevanceService;

    public GuardedAssistantRequestService(
            ConfiguredRequestBoundaryValidator boundaryValidator,
            TrustedRequestContextResolver contextResolver,
            RequestGuardrailService guardrailService,
            NlpAnalyzer nlpAnalyzer,
            NlpOperationAnalyzer operationAnalyzer,
            CatalogRelevanceService catalogRelevanceService
    ) {
        this.boundaryValidator = boundaryValidator;
        this.contextResolver = contextResolver;
        this.guardrailService = guardrailService;
        this.nlpAnalyzer = nlpAnalyzer;
        this.operationAnalyzer = operationAnalyzer;
        this.catalogRelevanceService = catalogRelevanceService;
    }

    @Override
    public AssistantRequestResult handle(AssistantRequestCommand command) {
        boundaryValidator.validate(
                command.tenantId(),
                command.userId(),
                command.conversationId(),
                command.designation(),
                command.question()
        );

        TrustedRequestContext context = contextResolver.resolve(
                command.tenantId(),
                command.userId(),
                command.conversationId(),
                command.correlationId()
        );
        log.info(
                "request_context correlationId={} requestId={} source={} productionTrusted={}",
                command.correlationId(),
                command.requestId(),
                context.source(),
                context.productionTrusted()
        );

        GuardrailDecision decision = guardrailService.evaluate(
                command.question(),
                command.correlationId()
        );
        NlpAnalysis analysis = null;
        List<NlpOperationFrame> frames = List.of();
        CatalogFrameValidation catalogValidation = null;
        String effectiveQuestion = null;
        CatalogRelevanceDecision relevanceDecision = null;
        if (decision.outcome() == GuardrailOutcome.ALLOW_TO_INTERPRET) {
            analysis = nlpAnalyzer.analyze(decision.normalizedQuestion());
            frames = operationAnalyzer.analyze(analysis);
            log.info(
                    "nlp_decomposition correlationId={} requestId={} unitCount={} effects={}",
                    command.correlationId(),
                    command.requestId(),
                    frames.size(),
                    frames.stream().map(NlpOperationFrame::effect).toList()
            );
            if (frames.stream().anyMatch(NlpOperationFrame::blocksRequest)) {
                decision = new GuardrailDecision(
                        GuardrailOutcome.BLOCKED_UNSUPPORTED_MUTATION,
                        decision.normalizedQuestion(),
                        GuardrailReasonCode.UNSUPPORTED_MUTATION,
                        decision.auditFingerprint()
                );
            } else {
                catalogValidation = catalogRelevanceService.validateFrames(
                        frames,
                        command.correlationId()
                );
                relevanceDecision = catalogValidation.decision();
                effectiveQuestion = catalogValidation.effectiveQuestion();
            }
        }
        return new AssistantRequestResult(
                command.correlationId(),
                command.requestId(),
                decision,
                relevanceDecision,
                analysis,
                frames,
                catalogValidation,
                effectiveQuestion
        );
    }
}
