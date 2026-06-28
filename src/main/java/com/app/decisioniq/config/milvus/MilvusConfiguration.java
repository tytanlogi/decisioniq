package com.app.decisioniq.config.milvus;

import io.milvus.client.MilvusClient;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfiguration {

    @Bean
    public MilvusClientV2 milvusClient(MilvusProperties milvusProperties) {
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder().uri(milvusProperties.uri());

        if (milvusProperties.token() != null && !milvusProperties.token().isBlank()) {
            builder.token(milvusProperties.token());
        }
        return new MilvusClientV2(builder.build());
    }

}