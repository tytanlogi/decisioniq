package com.app.decisioniq.agent.type;

import com.app.decisioniq.agent.constant.AgentConstants;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Component;

import java.util.Map;
//It is a LangGraph4j state model
@Component
public class DecisionIQAgent extends AgentState {

    public static String INTENT="intent";
    public static String ANSWER="answer";

    /**
     * Constructs an AgentState with the given initial data.
     *
     * @param initData the initial data for the agent state
     */
    public DecisionIQAgent(Map<String, Object> initData) {
        super(initData);
    }

    public String intent(){
        return this.<String>value(AgentConstants.INTENT).orElse(" ");
    }

    public String answer(){
        return this.<String>value(AgentConstants.ANSWER).orElse(" ");
    }
}
