package com.app.decisioniq.agent.config;

import com.app.decisioniq.agent.node.SampleAnswerNode;
import com.app.decisioniq.agent.node.SampleIntentNode;
import com.app.decisioniq.agent.node.SampleQuestionNode;
import com.app.decisioniq.agent.type.SampleAgent;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleAgentGraphConfiguration {

    @Bean
    public CompiledGraph<SampleAgent> sampleAgentGraph(SampleQuestionNode questionNode, SampleIntentNode parserQuestionNode,
                                                       SampleAnswerNode sampleAnswerNode ) throws GraphStateException {

        StateGraph<SampleAgent> graph=new StateGraph<>(SampleAgent::new);
        graph.addNode("question", AsyncNodeAction.node_async(questionNode));
        graph.addNode("parse_question", AsyncNodeAction.node_async(parserQuestionNode));
        graph.addNode("answer_question", AsyncNodeAction.node_async(sampleAnswerNode));

        graph.addEdge(GraphDefinition.START,"question");
        graph.addEdge("question","parse_question");
        graph.addEdge("parse_question","answer_question");
        graph.addEdge("answer_question",GraphDefinition.END);
        return graph.compile();
    }

}
