package com.app.decisioniq.service.planning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QueryPlanningService {

    public String planQuery(String parsedQuestionJson) {
        log.info("Planning query for parsed question {}", parsedQuestionJson);
        return "Query planned";
    }
}
