package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.catalog.CatalogRelevanceService;
import com.app.decisioniq.application.catalog.CatalogFrameValidation;
import com.app.decisioniq.application.guardrail.ConfiguredRequestBoundaryValidator;
import com.app.decisioniq.application.guardrail.RequestGuardrailService;
import com.app.decisioniq.application.nlp.NlpAnalysis;
import com.app.decisioniq.application.nlp.NlpAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
import com.app.decisioniq.domain.catalog.CatalogRelevanceOutcome;
import com.app.decisioniq.domain.context.RequestContextSource;
import com.app.decisioniq.domain.context.TrustedRequestContext;
import com.app.decisioniq.domain.context.TrustedRequestContextResolver;
import com.app.decisioniq.domain.guardrail.GuardrailDecision;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardedAssistantRequestServiceTest {

    @Mock
    private ConfiguredRequestBoundaryValidator boundaryValidator;

    @Mock
    private TrustedRequestContextResolver contextResolver;

    @Mock
    private RequestGuardrailService guardrailService;
    @Mock
    private NlpAnalyzer nlpAnalyzer;
    @Mock
    private NlpOperationAnalyzer operationAnalyzer;
    @Mock
    private CatalogRelevanceService catalogRelevanceService;

    @Test
    void coordinatesBoundaryContextAndGuardrailInOrder() {
        AssistantRequestCommand command = new AssistantRequestCommand(
                "tenant-a",
                "user-a",
                "Fraud Analyst",
                "conversation-a",
                "show TXN-006451",
                "correlation-a",
                "request-a"
        );
        TrustedRequestContext context = new TrustedRequestContext(
                "tenant-a",
                "user-a",
                "conversation-a",
                "correlation-a",
                RequestContextSource.DEVELOPMENT_REQUEST_FIELDS
        );
        GuardrailDecision decision = new GuardrailDecision(
                GuardrailOutcome.ALLOW_TO_INTERPRET,
                "show TXN-006451",
                GuardrailReasonCode.READY_FOR_INTERPRETATION,
                "fingerprint"
        );
        CatalogRelevanceDecision relevanceDecision = new CatalogRelevanceDecision(
                CatalogRelevanceOutcome.SUPPORTED,
                List.of(new Match("TRANSACTION_DETAILS", 1, 0.52))
        );
        when(contextResolver.resolve(
                "tenant-a",
                "user-a",
                "conversation-a",
                "correlation-a"
        )).thenReturn(context);
        when(guardrailService.evaluate("show TXN-006451", "correlation-a"))
                .thenReturn(decision);
        when(nlpAnalyzer.analyze("show TXN-006451"))
                .thenReturn(analysis("show TXN-006451"));
        NlpOperationFrame frame = new NlpOperationFrame(
                0, 0, "show TXN-006451", List.of("show"), List.of(), List.of(),
                NlpOperationFrame.Effect.SAFE_CANDIDATE
        );
        CatalogFrameValidation validation = new CatalogFrameValidation(
                relevanceDecision,
                List.of(new CatalogFrameValidation.UnitValidation(
                        frame,
                        CatalogRelevanceOutcome.SUPPORTED,
                        relevanceDecision.matches(),
                        true
                )),
                "show TXN-006451"
        );
        when(operationAnalyzer.analyze(analysis("show TXN-006451")))
                .thenReturn(List.of(frame));
        when(catalogRelevanceService.validateFrames(
                List.of(frame), "correlation-a"
        ))
                .thenReturn(validation);

        GuardedAssistantRequestService service = new GuardedAssistantRequestService(
                boundaryValidator,
                contextResolver,
                guardrailService,
                nlpAnalyzer,
                operationAnalyzer,
                catalogRelevanceService
        );
        AssistantRequestResult result = service.handle(command);

        assertThat(result.guardrailDecision()).isSameAs(decision);
        assertThat(result.correlationId()).isEqualTo("correlation-a");
        assertThat(result.requestId()).isEqualTo("request-a");
        assertThat(result.catalogRelevanceDecision()).isSameAs(relevanceDecision);
        assertThat(result.effectiveQuestion()).isEqualTo("show TXN-006451");

        InOrder order = inOrder(
                boundaryValidator, contextResolver, guardrailService, nlpAnalyzer,
                operationAnalyzer, catalogRelevanceService
        );
        order.verify(boundaryValidator).validate(
                "tenant-a",
                "user-a",
                "conversation-a",
                "Fraud Analyst",
                "show TXN-006451"
        );
        order.verify(contextResolver).resolve(
                "tenant-a",
                "user-a",
                "conversation-a",
                "correlation-a"
        );
        order.verify(guardrailService).evaluate("show TXN-006451", "correlation-a");
        order.verify(nlpAnalyzer).analyze("show TXN-006451");
        order.verify(operationAnalyzer).analyze(analysis("show TXN-006451"));
        order.verify(catalogRelevanceService).validateFrames(
                List.of(frame), "correlation-a"
        );
    }

    private NlpAnalysis analysis(String text) {
        NlpAnalysis.Clause clause = new NlpAnalysis.Clause(0, text, 0, text.length(), 1, 1);
        NlpAnalysis.Sentence sentence = new NlpAnalysis.Sentence(
                0, text, 0, text.length(), List.of(), List.of(), List.of(clause)
        );
        return new NlpAnalysis(text, List.of(sentence));
    }
}
