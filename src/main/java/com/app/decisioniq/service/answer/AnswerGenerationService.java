package com.app.decisioniq.service.answer;

import com.app.decisioniq.assistant.evidence.model.ExtractedData;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnswerGenerationService {

    private ChatModel chatModel;
    private ObjectMapper objectMapper;

    @Autowired
    public AnswerGenerationService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public String generateAnswer(String question, ParsedQuestion parsedQuestion, QueryPlan queryPlan, ExtractedData extractedData) {
        Map<String, String> stringMultiValueMapMap = mapJsonToIntent(extractedData);
        String finalPrompt = buildFinalPrompt(question, parsedQuestion, queryPlan, stringMultiValueMapMap);
        return chatModel.chat(finalPrompt);
    }

    private Map<String,String> mapJsonToIntent(ExtractedData extractedData){
        Map<String,String> intentToJsonMap=new HashMap<>();
        Map<String, MultiValueMap<String, List<Map<String, Object>>>> intentTableMap = extractedData.getSqlTableData().getIntentTableMap();
        for (Map.Entry<String,MultiValueMap<String, List<Map<String, Object>>>> intentValues:intentTableMap.entrySet()){
            intentToJsonMap.put(intentValues.getKey(),convertObjectToJson(intentValues.getValue()));
        }
        return intentToJsonMap;
    }

    private String convertObjectToJson(Object value){
        try{
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(value);
        }catch (JsonProcessingException e){
            throw new IllegalStateException("Failed to convert SQL evidence to JSON", e);
        }
    }

    private String buildFinalPrompt(String question, ParsedQuestion parsedQuestion, QueryPlan queryPlan, Map<String,String> intentToJsonMap){
        Map<String, Object> queryContext = new HashMap<>();
        queryContext.put("tenantId", queryPlan.getTenantId());
        queryContext.put("transactionId", queryPlan.getTransactionId());
        queryContext.put("intents", queryPlan.getIntents());

        return """
                You are DecisionIQ, a fraud decision intelligence assistant.

                Your job is to explain a fraud/transaction decision in plain business language.
                The SQL evidence below is machine evidence for your reasoning, not a response template.

                Grounding rules:
                - Use only the provided SQL evidence.
                Do not invent transaction facts, model scores, rules, reason codes, customer details, or policy details.
                If the evidence is empty or insufficient, say exactly what is missing.
                If the evidence supports an answer, explain what happened and why it matters.
                Do not expose raw JSON, table names, field names, or internal query-planning details unless the user asks for them.
                Do not list every available field. Select only the facts that explain the decision.

                User question:
                %s

                Parsed question:
                %s

                Query context:
                %s

                SQL evidence grouped by intent:
                %s

                Write the final answer as if you are explaining the decision to a fraud analyst.

                Response format:
                1. Start with one direct sentence that answers why the transaction was approved.
                2. Then provide 3 to 5 short bullets under "Why it was approved".
                3. Include only a small "Supporting details" section if needed for amount, channel, merchant, model score, rule, or reason codes.
                4. If there are gaps, add "What is missing" at the end.

                Style requirements:
                - Use natural language, not a schema dump.
                - Prefer business terms like "low fraud risk", "known device", "domestic purchase", and "low transaction amount".
                - Avoid labels like case_id, correlation_id, model_id, rule_name, known_device, or decision_action unless the exact identifier is important.
                - Do not output bullets that contain only a heading.
                - Keep the answer compact and readable.
                - Markdown is allowed.
                """.formatted(
                question,
                convertObjectToJson(parsedQuestion),
                convertObjectToJson(queryContext),
                convertObjectToJson(intentToJsonMap)
        );
    }


}
