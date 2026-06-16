package com.app.decisioniq.service.planning;

import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QueryPlanningService {

    public String planQuery(ParsedQuestion parsedQuestionJson) {
        log.info("Planning query for parsed question {}", parsedQuestionJson);
        return "Query planned";
    }
}
