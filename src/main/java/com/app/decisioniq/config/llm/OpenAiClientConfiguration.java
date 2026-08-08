package com.app.decisioniq.config.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "decisioniq.llm.openai", name = "enabled", havingValue = "true")
    OpenAIClient openAIClient(OpenAiProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .timeout(properties.timeout())
                .maxRetries(properties.maxRetries())
                .responseValidation(true)
                .build();
    }
}
