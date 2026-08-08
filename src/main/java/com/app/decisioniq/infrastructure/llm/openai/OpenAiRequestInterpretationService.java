package com.app.decisioniq.infrastructure.llm.openai;

import com.app.decisioniq.application.assistant.InterpretationInput;
import com.app.decisioniq.application.interpretation.InterpretationUnavailableException;
import com.app.decisioniq.application.interpretation.InterpretationValidator;
import com.app.decisioniq.application.interpretation.InvalidInterpretationException;
import com.app.decisioniq.application.interpretation.QueryInterpretation;
import com.app.decisioniq.application.interpretation.RequestInterpretationService;
import com.app.decisioniq.config.llm.LangfuseProperties;
import com.app.decisioniq.config.llm.OpenAiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseUsage;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.StructuredResponseOutputItem;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "decisioniq.llm.openai", name = "enabled", havingValue = "true")
public class OpenAiRequestInterpretationService implements RequestInterpretationService {

    private static final Logger log = LoggerFactory.getLogger(
            OpenAiRequestInterpretationService.class
    );

    private final OpenAIClient client;
    private final OpenAiProperties properties;
    private final LangfuseProperties langfuseProperties;
    private final OpenAiPromptFactory promptFactory;
    private final ExternalContentMasker contentMasker;
    private final OpenAiInterpretationMapper mapper;
    private final InterpretationValidator validator;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public OpenAiRequestInterpretationService(
            OpenAIClient client,
            OpenAiProperties properties,
            LangfuseProperties langfuseProperties,
            OpenAiPromptFactory promptFactory,
            ExternalContentMasker contentMasker,
            OpenAiInterpretationMapper mapper,
            InterpretationValidator validator,
            ObjectMapper objectMapper,
            Tracer tracer
    ) {
        this.client = client;
        this.properties = properties;
        this.langfuseProperties = langfuseProperties;
        this.promptFactory = promptFactory;
        this.contentMasker = contentMasker;
        this.mapper = mapper;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @Override
    public Optional<QueryInterpretation> interpret(
            InterpretationInput input,
            String correlationId,
            String requestId
    ) {
        ExternalContentMasker.MaskedContent masked = contentMasker.mask(
                promptFactory.create(input)
        );
        long outputTokenBudget = outputTokenBudget(input);
        Span span = startSpan(correlationId, requestId, masked.value());
        try (Scope ignored = span.makeCurrent()) {
            StructuredResponseCreateParams<OpenAiInterpretationOutput> params =
                    ResponseCreateParams.builder()
                            .model(properties.model())
                            .instructions(OpenAiPromptFactory.INSTRUCTIONS)
                            .input(masked.value())
                            .maxOutputTokens(outputTokenBudget)
                            .reasoning(Reasoning.builder()
                                    .effort(ReasoningEffort.MINIMAL)
                                    .build())
                            .store(false)
                            .text(OpenAiInterpretationOutput.class)
                            .build();
            log.info(
                    "openai_request correlationId={} requestId={} model={} promptVersion={} "
                            + "inputLength={} maxOutputTokens={} store=false structuredOutput={}",
                    correlationId,
                    requestId,
                    properties.model(),
                    properties.promptVersion(),
                    masked.value().length(),
                    outputTokenBudget,
                    OpenAiInterpretationOutput.class.getSimpleName()
            );
            if (log.isDebugEnabled()) {
                log.debug(
                        "openai_request_payload correlationId={} requestId={} payload={}",
                        correlationId,
                        requestId,
                        params.rawParams()._body()
                );
            }
            StructuredResponse<OpenAiInterpretationOutput> response =
                    client.responses().create(params);
            logResponse(response, correlationId, requestId);
            OpenAiInterpretationOutput output = response.output().stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(content -> content.outputText().stream())
                    .findFirst()
                    .orElseThrow(() -> missingStructuredOutput(response));
            QueryInterpretation interpretation = masked.restore(mapper.toDomain(
                    output,
                    properties.model(),
                    properties.promptVersion(),
                    response.id()
            ));
            validator.validate(input, interpretation);
            recordSuccess(span, response, interpretation);
            log.info(
                    "llm_interpretation correlationId={} requestId={} model={} disposition={} unitCount={}",
                    correlationId,
                    requestId,
                    properties.model(),
                    interpretation.disposition(),
                    interpretation.units().size()
            );
            return Optional.of(interpretation);
        } catch (InvalidInterpretationException exception) {
            recordFailure(span, exception);
            log.warn(
                    "llm_interpretation_rejected correlationId={} requestId={} reason={}",
                    correlationId,
                    requestId,
                    exception.getMessage()
            );
            throw exception;
        } catch (RuntimeException exception) {
            recordFailure(span, exception);
            Throwable rootCause = rootCause(exception);
            log.error(
                    "llm_interpretation_unavailable correlationId={} requestId={} "
                            + "exceptionType={} rootCauseType={} rootCauseMessage={}",
                    correlationId,
                    requestId,
                    exception.getClass().getSimpleName(),
                    rootCause.getClass().getSimpleName(),
                    safeMessage(rootCause.getMessage())
            );
            throw new InterpretationUnavailableException(
                    "OpenAI interpretation is temporarily unavailable",
                    exception
            );
        } finally {
            span.end();
        }
    }

    private Span startSpan(String correlationId, String requestId, String prompt) {
        Span span = tracer.spanBuilder("decisioniq.request.interpretation").startSpan();
        span.setAttribute("langfuse.trace.name", "decisioniq-request-interpretation");
        span.setAttribute("langfuse.observation.type", "generation");
        span.setAttribute("langfuse.observation.model.name", properties.model());
        span.setAttribute("langfuse.version", properties.promptVersion());
        span.setAttribute("langfuse.environment", langfuseProperties.environment());
        span.setAttribute("langfuse.release", langfuseProperties.release());
        span.setAttribute("langfuse.trace.metadata.correlation_id", correlationId);
        span.setAttribute("langfuse.trace.metadata.request_id", requestId);
        span.setAttribute("gen_ai.operation.name", "chat");
        span.setAttribute("gen_ai.provider.name", "openai");
        span.setAttribute("gen_ai.request.model", properties.model());
        if (langfuseProperties.captureContent()) {
            span.setAttribute("langfuse.observation.input", prompt);
        }
        return span;
    }

    private void recordSuccess(
            Span span,
            StructuredResponse<OpenAiInterpretationOutput> response,
            QueryInterpretation interpretation
    ) {
        span.setStatus(StatusCode.OK);
        span.setAttribute("gen_ai.response.model", properties.model());
        span.setAttribute("gen_ai.response.id", response.id());
        response.usage().ifPresent(usage -> recordUsage(span, usage));
        if (langfuseProperties.captureContent()) {
            try {
                span.setAttribute(
                        "langfuse.observation.output",
                        contentMasker.mask(objectMapper.writeValueAsString(interpretation)).value()
                );
            } catch (JsonProcessingException exception) {
                span.setAttribute("langfuse.observation.status_message", "Output serialization failed");
            }
        }
    }

    private void recordUsage(Span span, ResponseUsage usage) {
        span.setAttribute("gen_ai.usage.input_tokens", usage.inputTokens());
        span.setAttribute("gen_ai.usage.output_tokens", usage.outputTokens());
        span.setAttribute("gen_ai.usage.total_tokens", usage.totalTokens());
    }

    private void recordFailure(Span span, RuntimeException exception) {
        span.setStatus(StatusCode.ERROR, exception.getClass().getSimpleName());
        span.setAttribute("langfuse.observation.level", "ERROR");
        span.setAttribute(
                "langfuse.observation.status_message",
                exception.getClass().getSimpleName()
        );
    }

    private long outputTokenBudget(InterpretationInput input) {
        long unitCount = input.supportedUnits().size() + input.unsupportedUnits().size();
        return Math.min(properties.maxOutputTokens(), Math.max(256L, 256L + unitCount * 96L));
    }

    private void logResponse(
            StructuredResponse<OpenAiInterpretationOutput> response,
            String correlationId,
            String requestId
    ) {
        String status = response.status().map(value -> value.asString()).orElse("unknown");
        String incompleteReason = response.incompleteDetails()
                .flatMap(details -> details.reason())
                .map(reason -> reason.asString())
                .orElse("none");
        String errorCode = response.error()
                .map(error -> error.code().toString())
                .orElse("none");
        String outputKinds = response.output().stream()
                .map(this::outputKind)
                .toList()
                .toString();
        long inputTokens = response.usage().map(ResponseUsage::inputTokens).orElse(0L);
        long outputTokens = response.usage().map(ResponseUsage::outputTokens).orElse(0L);

        log.info(
                "openai_response correlationId={} requestId={} responseId={} status={} "
                        + "incompleteReason={} errorCode={} outputKinds={} inputTokens={} outputTokens={}",
                correlationId,
                requestId,
                response.id(),
                status,
                incompleteReason,
                errorCode,
                outputKinds,
                inputTokens,
                outputTokens
        );
        if (log.isDebugEnabled()) {
            log.debug(
                    "openai_response_payload correlationId={} requestId={} payload={}",
                    correlationId,
                    requestId,
                    response.rawResponse()
            );
        }
    }

    private String outputKind(StructuredResponseOutputItem<OpenAiInterpretationOutput> item) {
        if (item.isMessage()) {
            return "message";
        }
        if (item.isReasoning()) {
            return "reasoning";
        }
        return "other";
    }

    private InvalidInterpretationException missingStructuredOutput(
            StructuredResponse<OpenAiInterpretationOutput> response
    ) {
        String status = response.status().map(value -> value.asString()).orElse("unknown");
        String incompleteReason = response.incompleteDetails()
                .flatMap(details -> details.reason())
                .map(reason -> reason.asString())
                .orElse("none");
        boolean refused = response.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .anyMatch(content -> content.refusal().isPresent());
        return new InvalidInterpretationException(
                "OpenAI returned no structured interpretation: status=" + status
                        + ", incompleteReason=" + incompleteReason
                        + ", refused=" + refused
        );
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "none";
        }
        String singleLine = message.replace('\n', ' ').replace('\r', ' ');
        return singleLine.length() <= 500 ? singleLine : singleLine.substring(0, 500);
    }
}
