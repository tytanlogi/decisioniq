package com.app.decisioniq.service.semantic;

import com.app.decisioniq.api.admin.model.IntentRequestBody;
import com.app.decisioniq.config.milvus.MilvusProperties;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class SemanticService {

    private static final double SEMANTIC_MATCH_THRESHOLD = 0.35;
    private static final int SEMANTIC_SEARCH_LIMIT = 3;

    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;
    private final EmbeddingVectorService embeddingVectorService;

    public SemanticService(MilvusClientV2 milvusClient,MilvusProperties milvusProperties,
            EmbeddingVectorService embeddingVectorService) {
        this.milvusClient = milvusClient;
        this.milvusProperties = milvusProperties;
        this.embeddingVectorService = embeddingVectorService;
    }

    public void addIntent(IntentRequestBody intentRequestBody) {
        upsertSemanticText(intentRequestBody.getId(), intentRequestBody.getText());
    }

    public int addIntents(List<IntentRequestBody> intentRequestBodies) {
        if (intentRequestBodies == null || intentRequestBodies.isEmpty()) {
            throw new IllegalArgumentException("At least one semantic intent is required");
        }

        List<JsonObject> rows = new ArrayList<>(intentRequestBodies.size());
        for (IntentRequestBody intentRequestBody : intentRequestBodies) {
            rows.add(buildMilvusRow(intentRequestBody.getId(), intentRequestBody.getText()));
        }

        upsertRows(rows);
        return rows.size();
    }

    public Optional<IntentRequestBody> getIntent(String id) {
        var request = QueryReq.builder()
                .collectionName(milvusProperties.collectionName())
                .ids(List.of(id))
                .outputFields(List.of("id", "text"))
                .limit(1);

        if (StringUtils.hasText(milvusProperties.databaseName())) {
            request.databaseName(milvusProperties.databaseName());
        }

        var response = milvusClient.query(request.build());
        if (response.getQueryResults().isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> entity = response.getQueryResults().get(0).getEntity();
        IntentRequestBody intent = new IntentRequestBody();
        intent.setId(String.valueOf(entity.get("id")));
        intent.setText(String.valueOf(entity.get("text")));
        return Optional.of(intent);
    }

    public void upsertSemanticText(String id, String text) {
        upsertRows(List.of(buildMilvusRow(id, text)));
    }

    private JsonObject buildMilvusRow(String id, String text) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("Semantic intent id is required");
        }
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Semantic intent text is required");
        }

        JsonObject row = new JsonObject();
        row.addProperty("id", id);
        row.addProperty("text", text);
        row.add("vector", embeddingVectorService.generateMilvusVector(text));
        return row;
    }

    private void upsertRows(List<JsonObject> rows) {
        var request = UpsertReq.builder()
                .collectionName(milvusProperties.collectionName())
                .data(rows);

        if (StringUtils.hasText(milvusProperties.databaseName())) {
            request.databaseName(milvusProperties.databaseName());
        }

        milvusClient.upsert(request.build());
    }

    public boolean compareEmbeddings(String textToCompare){
        List<Float> queryVector = embeddingVectorService.generateEmbedding(textToCompare);
        SearchReq searchReq = SearchReq.builder()
                .collectionName(milvusProperties.collectionName())
                .data(List.of(new FloatVec(queryVector)))
                .annsField("vector")
                .limit(SEMANTIC_SEARCH_LIMIT)
                .outputFields(List.of("id", "text"))
                .build();
        SearchResp response = milvusClient.search(searchReq);

        if (response.getSearchResults().isEmpty() || response.getSearchResults().get(0).isEmpty()){
            return false;
        }

        SearchResp.SearchResult result = response.getSearchResults().get(0).get(0);
        double score=result.getScore();
        Object matchedCapability = result.getEntity().get("id");
        log.info("semantic matchedCapability {} score {}", matchedCapability, score);
        return score >= SEMANTIC_MATCH_THRESHOLD;
    }

}
