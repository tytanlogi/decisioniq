package com.app.decisioniq.assistant.graph.config;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphNode;
import com.app.decisioniq.assistant.graph.node.*;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.utils.EdgeMappings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class DecisionGraphConfiguration {

    @Bean
    public CompiledGraph<DecisionIQAgentState> decisionAssistantGraph(SemanticValidationNode semanticValidationNode,
                                                                      IntentUnderstandingNode intentUnderstandingNode,
                                                                      IntentClarificationNode intentClarificationNode,
                                                                      QueryPlanningNode queryPlanningNode,
                                                                      EvidenceCollectionNode evidenceCollectionNode,
                                                                      AnswerGenerationNode answerGenerationNode
    ) throws GraphStateException {
        StateGraph<DecisionIQAgentState> graph = new StateGraph<>(DecisionIQAgentState::new);

        graph.addNode(DecisionGraphNode.SEMANTIC_VALIDATION,AsyncNodeAction.node_async(semanticValidationNode));
        graph.addConditionalEdges(
                DecisionGraphNode.SEMANTIC_VALIDATION,
                AsyncEdgeAction.edge_async(this::routeAfterSemanticValidation),
                EdgeMappings.builder()
                        .to(DecisionGraphNode.INTENT_CLARIFICATION,"clarification")
                        .to(DecisionGraphNode.INTENT_UNDERSTANDING,"continue")
                        .build()
        );
        graph.addNode(DecisionGraphNode.INTENT_UNDERSTANDING, AsyncNodeAction.node_async(intentUnderstandingNode));
        graph.addNode(DecisionGraphNode.INTENT_CLARIFICATION, AsyncNodeAction.node_async(intentClarificationNode));
        graph.addNode(DecisionGraphNode.QUERY_PLANNING, AsyncNodeAction.node_async(queryPlanningNode));
        graph.addNode(DecisionGraphNode.EVIDENCE_COLLECTION,AsyncNodeAction.node_async(evidenceCollectionNode));
        graph.addNode(DecisionGraphNode.ANSWER_GENERATION, AsyncNodeAction.node_async(answerGenerationNode));

        graph.addEdge(GraphDefinition.START, DecisionGraphNode.SEMANTIC_VALIDATION);
        graph.addConditionalEdges(
                DecisionGraphNode.INTENT_UNDERSTANDING,
                AsyncEdgeAction.edge_async(this::routeAfterIntentClarification),
                EdgeMappings.builder()
                        .to(DecisionGraphNode.INTENT_CLARIFICATION,"clarification")
                        .to(DecisionGraphNode.QUERY_PLANNING,"continue")
                        .build()
        );
        graph.addEdge(DecisionGraphNode.INTENT_CLARIFICATION, GraphDefinition.END);
        graph.addEdge(DecisionGraphNode.QUERY_PLANNING, DecisionGraphNode.EVIDENCE_COLLECTION);
        graph.addEdge(DecisionGraphNode.EVIDENCE_COLLECTION,DecisionGraphNode.ANSWER_GENERATION);
        graph.addEdge(DecisionGraphNode.ANSWER_GENERATION, GraphDefinition.END);

        return graph.compile();
    }

    private String routeAfterSemanticValidation(DecisionIQAgentState state){
        boolean questionSemanticallyValid = state.isQuestionSemanticallyValid();
        if (!questionSemanticallyValid){
            return "clarification";
        }
        return "continue";
    }

    private String routeAfterIntentClarification(DecisionIQAgentState state){
        Optional<ParsedQuestion> parsedQuestion = state.clarifyIntent();
        if (parsedQuestion.isPresent() && parsedQuestion.get().isClarificationRequired()){
            return "clarification";
        }
        return "continue";
    }
}
