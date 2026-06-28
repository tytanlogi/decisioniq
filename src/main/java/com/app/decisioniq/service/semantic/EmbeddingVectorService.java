package com.app.decisioniq.service.semantic;

import com.google.gson.JsonArray;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class EmbeddingVectorService {

    private final LocalEmbeddingService localEmbeddingService;

    public EmbeddingVectorService(LocalEmbeddingService localEmbeddingService) {
        this.localEmbeddingService = localEmbeddingService;
    }

    public List<Float> generateEmbedding(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Text is required for embedding generation");
        }
        return localEmbeddingService.embed(text);
    }

    public JsonArray generateMilvusVector(String text) {
        return toJsonArray(generateEmbedding(text));
    }

    public JsonArray toJsonArray(List<Float> embedding) {
        JsonArray vector = new JsonArray();
        for (Float value : embedding) {
            vector.add(value);
        }
        return vector;
    }
}
