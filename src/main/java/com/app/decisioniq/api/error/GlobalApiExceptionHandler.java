package com.app.decisioniq.api.error;

import com.app.decisioniq.api.filter.RequestIdentityFilter;
import com.app.decisioniq.application.assistant.AssistantRequestResult;
import com.app.decisioniq.application.nlp.OperationPolicyOutcome;
import com.app.decisioniq.application.catalog.CatalogSearchUnavailableException;
import com.app.decisioniq.application.guardrail.RequestBoundaryValidationException;
import com.app.decisioniq.config.catalog.CatalogRelevanceProperties;
import com.app.decisioniq.config.guardrail.GuardrailProperties;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
import com.app.decisioniq.domain.catalog.CatalogRelevanceOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailOutcome;
import com.app.decisioniq.domain.guardrail.GuardrailReasonCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    private final GuardrailProperties properties;
    private final CatalogRelevanceProperties relevanceProperties;

    public GlobalApiExceptionHandler(
            GuardrailProperties properties,
            CatalogRelevanceProperties relevanceProperties
    ) {
        this.properties = properties;
        this.relevanceProperties = relevanceProperties;
    }

    @ExceptionHandler(DecisionIqApiException.class)
    public ResponseEntity<ApiErrorResponse> handleDecisionIqException(
            DecisionIqApiException exception,
            HttpServletRequest request
    ) {
        GuardrailOutcome outcome = exception.guardrailDecision() == null
                ? null
                : exception.guardrailDecision().outcome();
        GuardrailReasonCode reason = exception.guardrailDecision() == null
                ? null
                : exception.guardrailDecision().reasonCode();
        CatalogRelevanceDecision relevance = exception.catalogRelevanceDecision();
        AssistantRequestResult requestResult = exception.requestResult();
        OperationPolicyOutcome operationPolicyOutcome = requestResult == null
                ? OperationPolicyOutcome.NOT_EVALUATED
                : requestResult.operationPolicyOutcome();
        return error(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                request,
                outcome,
                reason,
                operationPolicyOutcome,
                relevance == null ? null : relevance.outcome(),
                relevance == null ? List.of() : relevance.matches(),
                List.of()
        );
    }

    @ExceptionHandler(CatalogSearchUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleCatalogUnavailable(
            CatalogSearchUnavailableException exception,
            HttpServletRequest request
    ) {
        CatalogRelevanceProperties.ResponseTemplate template =
                relevanceProperties.responses().unavailable();
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                template.code(),
                template.message(),
                request,
                GuardrailOutcome.ALLOW_TO_INTERPRET,
                GuardrailReasonCode.READY_FOR_INTERPRETATION,
                OperationPolicyOutcome.NOT_EVALUATED,
                null,
                List.of(),
                List.of()
        );
    }

    @ExceptionHandler(RequestBoundaryValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleConfiguredValidation(
            RequestBoundaryValidationException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldViolation> violations = exception.violations().stream()
                .map(violation -> new ApiFieldViolation(violation.field(), violation.message()))
                .sorted(Comparator.comparing(ApiFieldViolation::field))
                .toList();
        return invalidRequest(request, violations);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldViolation> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ApiFieldViolation(
                        fieldError.getField(),
                        fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage()
                ))
                .sorted(Comparator.comparing(ApiFieldViolation::field))
                .toList();
        return invalidRequest(request, violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        GuardrailProperties.ResponseTemplate template = properties.responses().requestInvalid();
        return error(
                HttpStatus.BAD_REQUEST,
                template.code(),
                template.message(),
                request,
                GuardrailOutcome.REQUEST_INVALID,
                GuardrailReasonCode.MALFORMED_INPUT,
                OperationPolicyOutcome.NOT_EVALUATED,
                null,
                List.of(),
                List.of()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        GuardrailProperties.ResponseTemplate template = properties.responses().unsupportedMediaType();
        return error(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                template.code(),
                template.message(),
                request,
                GuardrailOutcome.REQUEST_INVALID,
                GuardrailReasonCode.MALFORMED_INPUT,
                OperationPolicyOutcome.NOT_EVALUATED,
                null,
                List.of(),
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "api_failure correlationId={} requestId={} exceptionType={}",
                correlationId(request),
                requestId(request),
                exception.getClass().getSimpleName()
        );
        GuardrailProperties.ResponseTemplate template = properties.responses().internalError();
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                template.code(),
                template.message(),
                request,
                null,
                null,
                OperationPolicyOutcome.NOT_EVALUATED,
                null,
                List.of(),
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> invalidRequest(
            HttpServletRequest request,
            List<ApiFieldViolation> violations
    ) {
        GuardrailProperties.ResponseTemplate template = properties.responses().requestInvalid();
        return error(
                HttpStatus.BAD_REQUEST,
                template.code(),
                template.message(),
                request,
                GuardrailOutcome.REQUEST_INVALID,
                GuardrailReasonCode.MALFORMED_INPUT,
                OperationPolicyOutcome.NOT_EVALUATED,
                null,
                List.of(),
                violations
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            GuardrailOutcome outcome,
            GuardrailReasonCode reason,
            OperationPolicyOutcome operationPolicyOutcome,
            CatalogRelevanceOutcome relevanceOutcome,
            List<Match> catalogMatches,
            List<ApiFieldViolation> violations
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                correlationId(request),
                requestId(request),
                outcome,
                reason,
                operationPolicyOutcome,
                relevanceOutcome,
                catalogMatches,
                violations
        ));
    }

    private String correlationId(HttpServletRequest request) {
        return attribute(request, RequestIdentityFilter.CORRELATION_ATTRIBUTE);
    }

    private String requestId(HttpServletRequest request) {
        return attribute(request, RequestIdentityFilter.REQUEST_ATTRIBUTE);
    }

    private String attribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? "unavailable" : value.toString();
    }
}
