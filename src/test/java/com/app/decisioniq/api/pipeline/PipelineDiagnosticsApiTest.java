package com.app.decisioniq.api.pipeline;

import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
import com.app.decisioniq.infrastructure.catalog.CatalogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PipelineDiagnosticsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogClient catalogClient;

    @BeforeEach
    void catalogResponses() {
        when(catalogClient.search(anyString(), anyString()))
                .thenReturn(List.of(new Match("NO_MATCH", 1, 0.05)));
        when(catalogClient.search(
                org.mockito.ArgumentMatchers.eq(
                        "why was transaction tx:123232 was approved"
                ),
                anyString()
        )).thenReturn(List.of(new Match("DECISION_EXPLANATION", 1, 0.52)));
    }

    @Test
    void showsOnlyLlmQuestionAndRejectedParts() throws Exception {
        mockMvc.perform(post("/internal/dev/pipeline/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "why was transaction tx:123232 was approved and if it is then fuck off",
                                  "conversationId": "test-conversation",
                                  "tenantId": "tenant-citi-bank",
                                  "userId": "analyst-1"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionForLlm")
                        .value("why was transaction tx:123232 was approved"))
                .andExpect(jsonPath("$.rejectedParts.length()").value(1))
                .andExpect(jsonPath("$.rejectedParts[0].text")
                        .value("if it is then fuck off"))
                .andExpect(jsonPath("$.rejectedParts[0].classification")
                        .value("SAFE_CANDIDATE"))
                .andExpect(jsonPath("$.rejectedParts[0].reason").value("OUT_OF_SCOPE"))
                .andExpect(jsonPath("$.nlpAnalysis").doesNotExist())
                .andExpect(jsonPath("$.catalogValidation").doesNotExist());
    }

    @Test
    void removesDetachedNonDomainFragmentsAndKeepsSupportedContext() throws Exception {
        when(catalogClient.search(
                org.mockito.ArgumentMatchers.eq("Explain the decision"), anyString()
        )).thenReturn(List.of(new Match("DECISION_EXPLANATION", 1, 0.52)));
        when(catalogClient.search(
                org.mockito.ArgumentMatchers.eq("for TXN-006451."), anyString()
        )).thenReturn(List.of(new Match("TRANSACTION_DETAILS", 1, 0.45)));

        mockMvc.perform(post("/internal/dev/pipeline/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Explain the decision, damn it, for TXN-006451.",
                                  "conversationId": "test-conversation",
                                  "tenantId": "tenant-citi-bank",
                                  "userId": "analyst-1"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionForLlm")
                        .value("Explain the decision for TXN-006451."))
                .andExpect(jsonPath("$.rejectedParts.length()").value(1))
                .andExpect(jsonPath("$.rejectedParts[0].text").value("damn it"));
    }
}
