package com.app.decisioniq.config.milvus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "decisioniq.rag.vector-store")
public record MilvusProperties(
        String uri,
        String token,
        String databaseName,
        String collectionName
) { }
