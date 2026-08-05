package com.app.decisioniq.api.nlp;

import com.app.decisioniq.application.nlp.NlpAnalysis;
import com.app.decisioniq.application.nlp.NlpAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationAnalyzer;
import com.app.decisioniq.application.nlp.NlpOperationFrame;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile("!prod")
@RequestMapping("/internal/dev/nlp")
public class NlpDiagnosticsApi {

    private final NlpAnalyzer analyzer;
    private final NlpOperationAnalyzer operationAnalyzer;

    public NlpDiagnosticsApi(
            NlpAnalyzer analyzer,
            NlpOperationAnalyzer operationAnalyzer
    ) {
        this.analyzer = analyzer;
        this.operationAnalyzer = operationAnalyzer;
    }

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public NlpAnalysis analyze(@Valid @RequestBody AnalyzeRequest request) {
        return analyzer.analyze(request.question());
    }

    @PostMapping(
            value = "/operations",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<NlpOperationFrame> operations(@Valid @RequestBody AnalyzeRequest request) {
        return operationAnalyzer.analyze(analyzer.analyze(request.question()));
    }

    public record AnalyzeRequest(
            @NotBlank(message = "Question is required")
            @Size(max = 4000, message = "Question exceeds the diagnostics limit")
            String question
    ) { }
}
