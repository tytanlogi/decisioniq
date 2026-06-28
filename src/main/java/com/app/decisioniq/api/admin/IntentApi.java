package com.app.decisioniq.api.admin;

import com.app.decisioniq.api.admin.model.IntentRequestBody;
import com.app.decisioniq.service.semantic.SemanticService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class IntentApi {

    private final SemanticService semanticService;

    public IntentApi(SemanticService semanticService) {
        this.semanticService = semanticService;
    }

    @PostMapping("/semantic-intents")
    public void addIntent(@RequestBody IntentRequestBody intentRequestBody){
        semanticService.addIntent(intentRequestBody);
    }

    @PostMapping("/semantic-intents/bulk")
    public Map<String, Object> addIntents(@RequestBody List<IntentRequestBody> intentRequestBodies) {
        int insertedCount = semanticService.addIntents(intentRequestBodies);
        return Map.of("insertedCount", insertedCount);
    }

    @GetMapping("/semantic-intents/{id}")
    public ResponseEntity<IntentRequestBody> getIntent(@PathVariable String id) {
        return semanticService.getIntent(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
