package com.app.decisioniq.service.planning;

import com.app.decisioniq.assistant.dataslice.catalog.IntentToDataSliceMap;
import com.app.decisioniq.assistant.dataslice.config.AssistantDataSliceProperties;
import com.app.decisioniq.assistant.dataslice.config.DataSliceProperties;
import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import com.app.decisioniq.assistant.intent.model.ParsedAsk;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class QueryPlanningService {

    private final AssistantDataSliceProperties assistantDataSliceProperties;

    @Autowired
    public QueryPlanningService(AssistantDataSliceProperties assistantDataSliceProperties) {
        this.assistantDataSliceProperties = assistantDataSliceProperties;
    }

    public String planQuery(ParsedQuestion parsedQuestionJson) {
        log.info("Planning getQuery for parsed question {}", parsedQuestionJson);
        getDetails(parsedQuestionJson);
        return "Query planned";
    }


    private void getDetails(ParsedQuestion parsedQuestion){
        getDataSliceForIntent(parsedQuestion.getAsks());
    }

    private void getDataSliceForIntent(List<ParsedAsk> parsedAsks){
        for (ParsedAsk parsedAsk:parsedAsks){
            List<DecisionDataSlice> decisionDataSlices = IntentToDataSliceMap.slicesFor(parsedAsk.intent());
            getDataForEachSlices(decisionDataSlices);

        }
    }

    private void getDataForEachSlices(List<DecisionDataSlice> decisionDataSlicesList){
       for (DecisionDataSlice decisionDataSlice:decisionDataSlicesList){
           DataSliceProperties dataSliceProperties = assistantDataSliceProperties.getDataSlices().get(decisionDataSlice);
           log.info("dataslice {}",dataSliceProperties);
       }
    }

}
