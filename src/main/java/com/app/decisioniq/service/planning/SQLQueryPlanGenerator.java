package com.app.decisioniq.service.planning;

import com.app.decisioniq.assistant.dataslice.config.AssistantDataSliceProperties;
import com.app.decisioniq.assistant.dataslice.config.DataSliceProperties;
import com.app.decisioniq.assistant.dataslice.config.TableFieldsProperties;
import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SQLQueryPlanGenerator {

     private final AssistantDataSliceProperties assistantDataSliceProperties;

     @Autowired
    public SQLQueryPlanGenerator(AssistantDataSliceProperties assistantDataSliceProperties) {
        this.assistantDataSliceProperties = assistantDataSliceProperties;
    }

    public  void getDataForEachSQLSlices(DecisionIqIntent intent, List<DecisionDataSlice> decisionDataSlicesList, QueryPlan queryPlan){
        for (DecisionDataSlice decisionDataSlice:decisionDataSlicesList){
            DataSliceProperties dataSliceProperties = assistantDataSliceProperties.getDataSlices().get(decisionDataSlice);
            if (dataSliceProperties == null) {
                throw new IllegalStateException("No SQL data slice configured for " + decisionDataSlice);
            }
            buildSqlData(dataSliceProperties,queryPlan,intent);
        }
    }

    private static void buildSqlData(DataSliceProperties dataSliceProperties, QueryPlan queryPlan, DecisionIqIntent intent) {
        MultiValueMap<String,List<String>> intentTableFieldsMap = queryPlan.getSqlTableFieldsMap()
                .computeIfAbsent(intent.name(), key -> new LinkedMultiValueMap<>());

        for (Map.Entry<String, TableFieldsProperties> tableEntry:dataSliceProperties.tables().entrySet()){
            List<List<String>> existingFieldGroups = intentTableFieldsMap
                    .computeIfAbsent(tableEntry.getKey(), key -> new ArrayList<>());
            List<String> fields = new ArrayList<>(tableEntry.getValue().fields());
            if (!existingFieldGroups.contains(fields)) {
                existingFieldGroups.add(fields);
            }
        }
    }

}
