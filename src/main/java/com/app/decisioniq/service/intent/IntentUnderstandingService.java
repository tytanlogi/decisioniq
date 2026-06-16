package com.app.decisioniq.service.intent;

import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.intent.prompt.IntentPromptTemplate;
import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class IntentUnderstandingService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public IntentUnderstandingService(@Qualifier("openAiChatModel") ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public ParsedQuestion parseIntent(String question) throws JsonProcessingException {
        ParsedQuestion parsedQuestion;
        if (countWords(question) <= 3) {
            log.warn("Please provide a clearer fraud decision question.");
        }
        String response = chatModel.chat(buildPrompt(question));
        return parseAndFlagIntentResponse(response);
    }

    private String buildPrompt(String question) {
        String finalPrompt = """
                %s

                User question:
                %s
 
                Allowed intent names:
                %s
                """.formatted(
                IntentPromptTemplate.INSTRUCTIONS,
                question,
                Arrays.toString(DecisionIqIntent.values())
        );
        log.info("Final intent prompt {}", finalPrompt);
        return finalPrompt;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }

    private ParsedQuestion parseAndFlagIntentResponse(String response) throws JsonProcessingException {
        ParsedQuestion parsedResponse = parseJsonIntentResponse(response);
        validateIntentResponse(parsedResponse);
        return parsedResponse;
    }

    private ParsedQuestion parseJsonIntentResponse(String response) throws JsonProcessingException{
        return objectMapper.readValue(response, ParsedQuestion.class);
    }

    private void validateIntentResponse(ParsedQuestion parsedResponse){
        if (parsedResponse.isCud()){
            log.warn("Intent is having a CRUD operation");
        }
    }
}
