package com.app.decisioniq.external;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OpenApiRestClient {

    private final RestClient restClient;
    private final OPenAiProperties oPenAiProperties;

    @Autowired
    public OpenApiRestClient(RestClient restClient, OPenAiProperties oPenAiProperties){
        this.restClient=restClient;
        this.oPenAiProperties = oPenAiProperties;
    }

    public JsonNode ask(String request,String instructions){
        return restClient.post()
                .uri("/v1/responses")
                .body(getRequestBody(request,instructions))
                .retrieve()
                .body(JsonNode.class);
    }

    private Map<String,Object> getRequestBody(String request,String instructions){
       return Map.of("model",oPenAiProperties.model(),
                "instructions",instructions,
                "input",request);
    }
}
