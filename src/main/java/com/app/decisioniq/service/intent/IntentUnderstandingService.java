package com.app.decisioniq.service.intent;

import com.app.decisioniq.assistant.intent.model.DecisionIqIntent;
import com.app.decisioniq.assistant.intent.prompt.IntentPromptTemplate;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class IntentUnderstandingService {

    private final ChatModel chatModel;

    public IntentUnderstandingService(@Qualifier("openAiChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String parseIntent(String question) {
        if (countWords(question) <= 3) {
            return """
                    {
                      "status": "CLARIFICATION_REQUIRED",
                      "transactionId": null,
                      "asks": [
                        {
                          "intent": "CLARIFICATION_REQUIRED",
                          "transactionId": null
                        }
                      ],
                      "decisionAssumption": null,
                      "clarificationRequired": true,
                      "clarificationQuestion": "Please provide a clearer fraud decision question."
                    }
                    """;
        }

        return chatModel.chat(buildPrompt(question));
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
}
