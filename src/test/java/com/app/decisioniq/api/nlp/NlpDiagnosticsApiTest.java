package com.app.decisioniq.api.nlp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NlpDiagnosticsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesClauseAnalysisForDevelopmentTesting() throws Exception {
        mockMvc.perform(post("/internal/dev/nlp/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Why was TXN-006451 approved and what was its model score?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentences[0].clauses.length()").value(2))
                .andExpect(jsonPath("$.sentences[0].clauses[0].text")
                        .value("Why was TXN-006451 approved"))
                .andExpect(jsonPath("$.sentences[0].clauses[1].text")
                        .value("what was its model score?"));
    }

    @Test
    void exposesCanonicalOperationFramesForDevelopmentTesting() throws Exception {
        mockMvc.perform(post("/internal/dev/nlp/operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Create a table in the database"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actions[0]").value("create"))
                .andExpect(jsonPath("$[0].objects[0]").value("table"))
                .andExpect(jsonPath("$[0].targets[0]").value("table"))
                .andExpect(jsonPath("$[0].targets[1]").value("database"))
                .andExpect(jsonPath("$[0].effect").value("PERSIST"))
                .andExpect(jsonPath("$[0].certainty").value("EXPLICIT"));
    }
}
