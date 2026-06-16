package com.app.decisioniq.assistant.graph.config;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphNode;
import com.app.decisioniq.assistant.graph.node.AnswerGenerationNode;
import com.app.decisioniq.assistant.graph.node.IntentClarificationNode;
import com.app.decisioniq.assistant.graph.node.IntentUnderstandingNode;
import com.app.decisioniq.assistant.graph.node.QueryPlanningNode;
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
    public CompiledGraph<DecisionIQAgentState> decisionAssistantGraph(IntentUnderstandingNode intentUnderstandingNode,
            IntentClarificationNode intentClarificationNode,
            QueryPlanningNode queryPlanningNode,
            AnswerGenerationNode answerGenerationNode
    ) throws GraphStateException {
        StateGraph<DecisionIQAgentState> graph = new StateGraph<>(DecisionIQAgentState::new);

        graph.addNode(DecisionGraphNode.INTENT_UNDERSTANDING, AsyncNodeAction.node_async(intentUnderstandingNode));
        graph.addNode(DecisionGraphNode.INTENT_CLARIFICATION, AsyncNodeAction.node_async(intentClarificationNode));
        graph.addNode(DecisionGraphNode.QUERY_PLANNING, AsyncNodeAction.node_async(queryPlanningNode));
        graph.addNode(DecisionGraphNode.ANSWER_GENERATION, AsyncNodeAction.node_async(answerGenerationNode));

        graph.addEdge(GraphDefinition.START, DecisionGraphNode.INTENT_UNDERSTANDING);
        graph.addConditionalEdges(
                DecisionGraphNode.INTENT_UNDERSTANDING,
                AsyncEdgeAction.edge_async(this::routeAfterIntentClarification),
                EdgeMappings.builder()
                        .to(DecisionGraphNode.INTENT_CLARIFICATION,"clarification")
                        .to(DecisionGraphNode.QUERY_PLANNING,"continue")
                        .build()
        );
        graph.addEdge(DecisionGraphNode.INTENT_CLARIFICATION, GraphDefinition.END);
        graph.addEdge(DecisionGraphNode.QUERY_PLANNING, DecisionGraphNode.ANSWER_GENERATION);
        graph.addEdge(DecisionGraphNode.ANSWER_GENERATION, GraphDefinition.END);

        return graph.compile();
    }

    private String routeAfterIntentClarification(DecisionIQAgentState state){
        Optional<ParsedQuestion> parsedQuestion = state.clarifyIntent();
        if (parsedQuestion.isPresent() && parsedQuestion.get().isClarificationRequired()){
            return "clarification";
        }
        return "continue";
    }
}
