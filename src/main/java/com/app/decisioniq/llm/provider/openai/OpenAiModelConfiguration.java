package com.app.decisioniq.llm.provider.openai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiResponsesChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
@Slf4j
public class OpenAiModelConfiguration {

    @Bean
    public ChatModel openAiChatModel(OpenAiProperties properties) {
        return OpenAiResponsesChatModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.model())
                .temperature(0.0)
                .baseUrl(properties.baseUrl())
                .build();
    }
}
