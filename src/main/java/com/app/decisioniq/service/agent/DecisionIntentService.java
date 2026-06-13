package com.app.decisioniq.service.agent;

import com.app.decisioniq.llm.DecisionIqIntent;
import com.app.decisioniq.llm.Prompts;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class DecisionIntentService {


    private final ChatModel chatModel;

    @Autowired
    public DecisionIntentService(@Qualifier("openAiChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String validateIntentAndAction(String question){
        String response="Not a valid question, should have more than 3 words";
        if (countWords(question)>3){
            response = chatModel.chat(buildPrompt(question));
        }
        return response;
    }

    private String buildPrompt(String question){
        String finalPrompt = """
                %s

                User question:
                %s

                Allowed intent names:
                %s
                """.formatted(
                Prompts.INTENT_INSTRUCTIONS,
                question,
                Arrays.toString(DecisionIqIntent.values())
        );
        log.info("Final prompt {}",finalPrompt);
        return finalPrompt;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }

}
