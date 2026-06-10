package com.app.decisioniq.agent.node;

import com.app.decisioniq.agent.type.SampleAgent;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SampleQuestionNode implements NodeAction<SampleAgent> {

    @Override
    public Map<String, Object> apply(SampleAgent state) {
        log.info("Parsing your question");
        return Map.of(SampleAgent.INTENT,state.question());
    }
}
