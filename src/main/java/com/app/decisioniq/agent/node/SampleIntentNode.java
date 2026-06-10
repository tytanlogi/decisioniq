package com.app.decisioniq.agent.node;

import com.app.decisioniq.agent.type.SampleAgent;
import com.app.decisioniq.external.OpenApiRestClient;
import com.app.decisioniq.llm.SampleIntent;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
@Slf4j
public class SampleIntentNode implements NodeAction<SampleAgent> {

    private final OpenApiRestClient openApiRestClient;

    public SampleIntentNode(OpenApiRestClient openApiRestClient){
        this.openApiRestClient=openApiRestClient;
    }

    @Override
    public Map<String, Object> apply(SampleAgent state) {
        log.info("Refining the intent of the question");
        String refinedQuestion = understandTheIntentOfQuestion(state.question());
        log.info("Refined question is {}",refinedQuestion);
        return Map.of(SampleAgent.INTENT,refinedQuestion);
    }

    private String understandTheIntentOfQuestion(String question){
        JsonNode ask = openApiRestClient.ask(buildQuestionToGetTheIntent(question), "think and map it correctly and frame a right and refined question to ask an output should contain the intent and question and nothing else");
        return ask.path("output")
                .path(0).
                path("content")
                .path(0)
                .path("text")
                .asText();
    }

    private String buildQuestionToGetTheIntent(String question){
        return "map all the related intents of the question->" + question + "-> predefined intents->" + Arrays.toString(SampleIntent.values());
    }
}
