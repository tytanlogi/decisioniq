package com.app.decisioniq.application.assistant;

import com.app.decisioniq.application.catalog.CatalogInterpretationInputFactory;
import com.app.decisioniq.application.interpretation.QueryInterpretation;
import com.app.decisioniq.application.interpretation.RequestInterpretationService;
import com.app.decisioniq.application.nlp.NlpAnalysis;
import com.app.decisioniq.application.nlp.NlpAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssistantInterpretationWorkflow implements AssistantInterpretationStage {

    private static final Logger log = LoggerFactory.getLogger(AssistantInterpretationWorkflow.class);

    private final NlpAnalyzer nlpAnalyzer;
    private final NlpOperationAnalyzer operationAnalyzer;
    private final CatalogInterpretationInputFactory inputFactory;
    private final RequestInterpretationService interpretationService;

    /**
     * Creates the post-guardrail interpretation workflow from its independent processing stages.
     */
    public AssistantInterpretationWorkflow(
            NlpAnalyzer nlpAnalyzer,
            NlpOperationAnalyzer operationAnalyzer,
            CatalogInterpretationInputFactory inputFactory,
            RequestInterpretationService interpretationService
    ) {
        this.nlpAnalyzer = nlpAnalyzer;
        this.operationAnalyzer = operationAnalyzer;
        this.inputFactory = inputFactory;
        this.interpretationService = interpretationService;
    }

    /**
     * Decomposes an allowed question, blocks unsafe operations, and requests a bounded interpretation.
     */
    @Override
    public AssistantInterpretationResult interpret(
            AssistantRequestCommand command,
            String normalizedQuestion
    ) {
        // Decompose the normalized question and classify the effect of every request unit.
        List<NlpOperationFrame> frames = analyzeOperations(command, normalizedQuestion);

        // Stop the complete request before catalog or LLM calls when any unit requests an unsupported operation.
        if (frames.stream().anyMatch(NlpOperationFrame::blocksRequest)) {
            return AssistantInterpretationResult.blocked();
        }

        // Resolve catalog candidates and construct the bounded input allowed to reach the LLM.
        InterpretationInput input = inputFactory.create(
                normalizedQuestion,
                frames,
                command.correlationId()
        );

        // Ask the configured interpreter for structured meaning while preserving request trace identifiers.
        QueryInterpretation interpretation = interpretationService.interpret(
                input,
                command.correlationId(),
                command.requestId()
        ).orElse(null);

        // Return both the exact model input and its optional validated interpretation.
        return AssistantInterpretationResult.allowed(input, interpretation);
    }

    /**
     * Runs NLP decomposition and operation analysis, then records only structural diagnostics.
     */
    private List<NlpOperationFrame> analyzeOperations(
            AssistantRequestCommand command,
            String normalizedQuestion
    ) {
        // Produce linguistic structure such as sentences, clauses, tokens, and dependencies.
        NlpAnalysis analysis = nlpAnalyzer.analyze(normalizedQuestion);

        // Convert the linguistic structure into operation-aware request units.
        List<NlpOperationFrame> frames = operationAnalyzer.analyze(analysis);

        // Audit only structural outcomes; raw question content is intentionally excluded.
        log.info(
                "nlp_decomposition correlationId={} requestId={} unitCount={} effects={}",
                command.correlationId(),
                command.requestId(),
                frames.size(),
                frames.stream().map(NlpOperationFrame::effect).toList()
        );
        return frames;
    }
}
