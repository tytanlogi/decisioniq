package com.app.decisioniq.external;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OPenAiProperties.class)
public class OpenAIRestClientConfiguration {

    @Bean
    public RestClient openAiRestClientConfig(RestClient.Builder builder,OPenAiProperties properties){
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer "+ properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
