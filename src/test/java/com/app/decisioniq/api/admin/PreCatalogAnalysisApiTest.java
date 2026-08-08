package com.app.decisioniq.api.admin;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class PreCatalogAnalysisApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsOneLookupPayloadWithSameMessageContextInsteadOfSearchingTheContextStatement() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Transaction TXN-12345 was approved.\nWhat was the model score?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("READY_FOR_CATALOG"))
                .andExpect(jsonPath("$.units[0].role").value("CONTEXT_STATEMENT"))
                .andExpect(jsonPath("$.units[1].role").value("EXECUTABLE_REQUEST"))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(1))
                .andExpect(jsonPath("$.catalogSearchRequests[0].query").value("What was the model score?"))
                .andExpect(jsonPath("$.catalogSearchRequests[0].resolvedContext.transactionId").value("TXN-12345"))
                .andExpect(jsonPath("$.catalogSearchRequests[0].resolvedContext.assertedOutcome").value("APPROVED"));
    }

    @Test
    void stopsBeforeCatalogPayloadWhenAnOperationIsBlocked() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Transaction TXN-12345 was approved.\nPlease email this report.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("BLOCKED_BY_OPERATION_POLICY"))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(0));
    }

    @Test
    void groupsRelatedExplicitTransactionLookupsIntoOneComparisonPayload() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Why was TX-12345 approved and why was TX-42342 not approved even though model score is same")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("READY_FOR_CATALOG"))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(1))
                .andExpect(jsonPath("$.catalogSearchRequests[0].requestKind").value("COMPARISON"))
                .andExpect(jsonPath("$.catalogSearchRequests[0].comparisonAspect").value("MODEL_SCORE"))
                .andExpect(jsonPath("$.catalogSearchRequests[0].lookupUnits.length()").value(2))
                .andExpect(jsonPath("$.catalogSearchRequests[0].lookupUnits[0].transactionId").value("TX-12345"))
                .andExpect(jsonPath("$.catalogSearchRequests[0].lookupUnits[1].transactionId").value("TX-42342"));
    }

    @Test
    void allowsHistoricalApprovalQuestionRatherThanTreatingApprovedAsAWriteCommand() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Can you tell me why TX-12345 was approved even though model score risk is too high")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("READY_FOR_CATALOG"))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(1))
                .andExpect(jsonPath("$.catalogSearchRequests[0].requestKind").value("LOOKUP"));
    }

    @Test
    void groupsTwoIdsInOneComparisonClauseWithAFollowingExplanationClause() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Can we compare two transaction TX-12345 and TX-45345 and explain me the difference")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("READY_FOR_CATALOG"))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(1))
                .andExpect(jsonPath("$.catalogSearchRequests[0].requestKind").value("COMPARISON"))
                .andExpect(jsonPath("$.catalogSearchRequests[0].lookupUnits.length()").value(2))
                .andExpect(jsonPath("$.catalogSearchRequests[0].lookupUnits[0].transactionIds.length()").value(2))
                .andExpect(jsonPath("$.catalogSearchRequests[0].lookupUnits[0].transactionIds[0]").value("TX-12345"))
                .andExpect(jsonPath("$.catalogSearchRequests[0].lookupUnits[0].transactionIds[1]").value("TX-45345"))
                .andExpect(jsonPath("$.catalogSearchRequests[0].comparisonAspect").value("UNSPECIFIED"));
    }

    @Test
    void ignoresConfiguredHarmlessNoiseInsteadOfSearchingIt() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("I am really confused about this one. Transaction TX-12353 was approved. Could you please show the rule that fired?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("READY_FOR_CATALOG"))
                .andExpect(jsonPath("$.units[0].role").value("IGNORED_HARMLESS_NOISE"))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(1))
                .andExpect(jsonPath("$.catalogSearchRequests[0].query").value("Could you please show the rule that fired?"));
    }

    @Test
    void inheritsExplicitLookupIdForAFollowingPronounLookup() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Show the details for TX-12352 and then explain why it was approved.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(2))
                .andExpect(jsonPath("$.catalogSearchRequests[1].resolvedContext.transactionId").value("TX-12352"))
                .andExpect(jsonPath("$.catalogSearchRequests[1].resolvedContext.sourceUnitIndex").value(0));
    }

    @Test
    void returnsClarificationInsteadOfGuessingAMissingComparisonReference() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Why did TX-12355 pass while the other transaction was declined? Compare them.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("CLARIFY_REQUIRED"))
                .andExpect(jsonPath("$.clarificationReason").value("MISSING_COMPARISON_REFERENCE"))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(0));
    }

    @Test
    void splitsCommaAndConjunctionSeparatedReadActionsIntoIndependentLookups() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Transaction TX-12356 was declined. Explain the decision, show the model score, and list the risk signals.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(3))
                .andExpect(jsonPath("$.catalogSearchRequests[0].query").value("Explain the decision"))
                .andExpect(jsonPath("$.catalogSearchRequests[1].query").value("show the model score"))
                .andExpect(jsonPath("$.catalogSearchRequests[2].query").value("list the risk signals."));
    }

    @Test
    void keepsSafePresentationConstructRequestsExecutable() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Create a table showing the decision details for TX-12358.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("READY_FOR_CATALOG"))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(1));
    }

    @Test
    void stopsBeforeNlpWhenTheGuardrailFindsRawSql() throws Exception {
        mockMvc.perform(post("/api/admin/pre-catalog-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Show TXN-12345. select *from transaction")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition").value("BLOCKED_BY_GUARDRAIL"))
                .andExpect(jsonPath("$.units.length()").value(0))
                .andExpect(jsonPath("$.catalogSearchRequests.length()").value(0));
    }

    private String json(String question) throws Exception {
        return objectMapper.writeValueAsString(new AssistantAskRequest(
                "tenant-test", "user-test", "analyst", "conversation-test", question
        ));
    }
}
