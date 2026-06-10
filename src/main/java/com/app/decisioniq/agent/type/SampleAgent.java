package com.app.decisioniq.agent.type;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

public class SampleAgent extends AgentState {

    public static final String INTENT ="Intent";
    public static final String  QUESTION="Question";
    public static final String  ANSWER="Answer";

    /**
     * Constructs an AgentState with the given initial data.
     *
     * @param initData the initial data for the agent state
     */
    public SampleAgent(Map<String, Object> initData) {
        super(initData);
    }

    public String question(){
        return this.<String>value(QUESTION).orElse(" ");
    }

    public String parsedQuestion(){
        return this.<String>value(INTENT).orElse("");
    }

    public String answer() {
        return this.<String>value(ANSWER)
                .orElseThrow(() -> new IllegalStateException("Answer missing from graph state"));
    }

}
