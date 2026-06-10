package com.app.decisioniq.agent.node;

import com.app.decisioniq.agent.type.SampleAgent;
import com.app.decisioniq.external.OpenApiRestClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SampleAnswerNode implements NodeAction<SampleAgent> {

    private final OpenApiRestClient openApiRestClient;

    @Autowired
    public SampleAnswerNode(OpenApiRestClient openApiRestClient) {
        this.openApiRestClient = openApiRestClient;
    }

    @Override
    public Map<String, Object> apply(SampleAgent state) {
        log.info("Answering your question");
        JsonNode ask = openApiRestClient.ask(state.parsedQuestion(),"Give me the clear answer");
        String answer = ask.path("output")
                .path(0)
                .path("content")
                .path(0)
                .path("text")
                .asText();
        return Map.of(SampleAgent.ANSWER,answer);
    }
}
