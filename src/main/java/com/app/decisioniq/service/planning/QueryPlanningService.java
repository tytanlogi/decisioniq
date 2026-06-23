package com.app.decisioniq.service.planning;

import com.app.decisioniq.assistant.dataslice.catalog.IntentToDataSliceMap;
import com.app.decisioniq.assistant.dataslice.config.AssistantDataSliceProperties;
import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import com.app.decisioniq.assistant.intent.model.ParsedAsk;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import com.app.decisioniq.assistant.planning.catalog.IntentToRagChunkTypeMap;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class QueryPlanningService {

    private final SQLQueryPlanGenerator sqlQueryPlanGenerator;

    @Autowired
    public QueryPlanningService( SQLQueryPlanGenerator sqlQueryPlanGenerator) {
        this.sqlQueryPlanGenerator = sqlQueryPlanGenerator;
    }

    public QueryPlan planQuery(ParsedQuestion parsedQuestionJson,String tenantId) {
        log.info("Planning getQuery for parsed question {}", parsedQuestionJson);
        QueryPlan queryPlan=buildQueryPlanMetaData(parsedQuestionJson,tenantId);
        getDetails(parsedQuestionJson,queryPlan);
        return queryPlan;
    }

    private QueryPlan buildQueryPlanMetaData(ParsedQuestion parsedQuestionJson,String tenantId){
        QueryPlan queryPlan =new QueryPlan();
        queryPlan.setTenantId(tenantId);
        queryPlan.setTransactionId(parsedQuestionJson.getTransactionId());
        return queryPlan;
    }

    private void getDetails(ParsedQuestion parsedQuestion,QueryPlan queryPlan){
        getDataSliceForIntent(parsedQuestion.getAsks(),queryPlan);
    }

    private void getDataSliceForIntent(List<ParsedAsk> parsedAsks,QueryPlan queryPlan){
        List<DecisionIqIntent> decisionIqIntentList=new ArrayList<>();
        for (ParsedAsk parsedAsk:parsedAsks){
            List<DecisionDataSlice> decisionDataSlices = IntentToDataSliceMap.slicesFor(parsedAsk.intent());
            sqlQueryPlanGenerator.getDataForEachSQLSlices(parsedAsk.intent(),decisionDataSlices,queryPlan);
            List<String> ragChunkList = IntentToRagChunkTypeMap.chunkTypesFor(parsedAsk.intent());
            RAGQueryGenerator.buildRAGQueryPlan(ragChunkList,queryPlan,parsedAsk.intent());
            decisionIqIntentList.add(parsedAsk.intent());
        }
        queryPlan.setIntents(decisionIqIntentList);
    }

}
