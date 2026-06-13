package com.app.decisioniq.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DecisionQueryService {

    public String startQuery(String query){
        log.info("Query Received {}",query);
        return "Query Answered";
    }
}
