package com.app.decisioniq.assistant.graph.config;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphNode;
import com.app.decisioniq.assistant.graph.node.AnswerGenerationNode;
import com.app.decisioniq.assistant.graph.node.IntentUnderstandingNode;
import com.app.decisioniq.assistant.graph.node.QueryPlanningNode;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DecisionGraphConfiguration {

    @Bean
    public CompiledGraph<DecisionIQAgentState> decisionAssistantGraph(
            IntentUnderstandingNode intentUnderstandingNode,
            QueryPlanningNode queryPlanningNode,
            AnswerGenerationNode answerGenerationNode
    ) throws GraphStateException {
        StateGraph<DecisionIQAgentState> graph = new StateGraph<>(DecisionIQAgentState::new);

        graph.addNode(DecisionGraphNode.INTENT_UNDERSTANDING, AsyncNodeAction.node_async(intentUnderstandingNode));
        graph.addNode(DecisionGraphNode.QUERY_PLANNING, AsyncNodeAction.node_async(queryPlanningNode));
        graph.addNode(DecisionGraphNode.ANSWER_GENERATION, AsyncNodeAction.node_async(answerGenerationNode));

        graph.addEdge(GraphDefinition.START, DecisionGraphNode.INTENT_UNDERSTANDING);
        graph.addEdge(DecisionGraphNode.INTENT_UNDERSTANDING, DecisionGraphNode.QUERY_PLANNING);
        graph.addEdge(DecisionGraphNode.QUERY_PLANNING, DecisionGraphNode.ANSWER_GENERATION);
        graph.addEdge(DecisionGraphNode.ANSWER_GENERATION, GraphDefinition.END);

        return graph.compile();
    }
}
