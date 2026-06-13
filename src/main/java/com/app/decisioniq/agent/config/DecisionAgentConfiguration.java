package com.app.decisioniq.agent.config;

import com.app.decisioniq.agent.constant.AgentConstants;
import com.app.decisioniq.agent.node.decison.DIQ_AnswerAgent;
import com.app.decisioniq.agent.node.decison.DIQ_IntentUnderstandingAgent;
import com.app.decisioniq.agent.type.DecisionIQAgent;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DecisionAgentConfiguration {

    @Bean
    public CompiledGraph<DecisionIQAgent> ConfigureAgent(DIQ_IntentUnderstandingAgent intentUnderstandingAgent, DIQ_AnswerAgent answerAgent) throws GraphStateException {
        StateGraph<DecisionIQAgent> decisionIQAgentStateGraph=new StateGraph<>(DecisionIQAgent::new);

        decisionIQAgentStateGraph.addNode(AgentConstants.INTENT, AsyncNodeAction.node_async(intentUnderstandingAgent));
        decisionIQAgentStateGraph.addNode(AgentConstants.ANSWER, AsyncNodeAction.node_async(answerAgent));

        decisionIQAgentStateGraph.addEdge(GraphDefinition.START,AgentConstants.INTENT);
        decisionIQAgentStateGraph.addEdge(AgentConstants.INTENT,AgentConstants.ANSWER);
        decisionIQAgentStateGraph.addEdge(AgentConstants.ANSWER,GraphDefinition.END);
        return decisionIQAgentStateGraph.compile();
    }
}
