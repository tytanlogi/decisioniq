package com.app.decisioniq.config;

import com.app.decisioniq.external.OPenAiProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiResponsesChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(OPenAiProperties.class)
@Primary
@Slf4j
public class OpenAILangChainConfiguration {

    @Bean
    public ChatModel openAiChatModel(OPenAiProperties properties){
        log.info("Using OpenAI model: {}", properties.model());
        log.info("Using OpenAI base URL: {}", properties.baseUrl());
        return OpenAiResponsesChatModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.model())
                .temperature(0.0)
                .baseUrl(properties.baseUrl())
                .build();
    }
}
