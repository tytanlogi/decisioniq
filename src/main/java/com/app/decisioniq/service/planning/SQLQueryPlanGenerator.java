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
            buildSqlData(dataSliceProperties,queryPlan,intent);
        }
    }

    private static void buildSqlData(DataSliceProperties dataSliceProperties, QueryPlan queryPlan, DecisionIqIntent intent) {
        Map<String, TableFieldsProperties> tables = dataSliceProperties.tables();

        for (Map.Entry<String, TableFieldsProperties> tableFieldsPropertiesEntry:tables.entrySet()){
            if (!queryPlan.getSqlTableFieldsMap().isEmpty() && queryPlan.getSqlTableFieldsMap().get(intent.name()).containsKey(tableFieldsPropertiesEntry.getKey())){
                validateAndAddValue(queryPlan, tableFieldsPropertiesEntry,intent);
            }else {
                MultiValueMap<String,List<String>> tableFieldsMap=new LinkedMultiValueMap<>();
                List<List<String>> fields=new ArrayList<>();
                fields.add(new ArrayList<>(tableFieldsPropertiesEntry.getValue().fields()));
                tableFieldsMap.put(tableFieldsPropertiesEntry.getKey(),fields);
                if (queryPlan.getSqlTableFieldsMap().get(intent.name())==null){
                    queryPlan.getSqlTableFieldsMap().put(intent.name(),tableFieldsMap);
                }else if (!queryPlan.getSqlTableFieldsMap().get(intent.name()).isEmpty()){
                    queryPlan.getSqlTableFieldsMap().get(intent.name()).addAll(tableFieldsMap);
                }
            }
        }
    }

    private static void validateAndAddValue(QueryPlan queryPlan, Map.Entry<String, TableFieldsProperties> tableFieldsPropertiesEntry, DecisionIqIntent intent) {
        MultiValueMap<String, List<String>> multiValueTableFieldsMap = queryPlan.getSqlTableFieldsMap().get(intent.name());
        List<List<String>> existingTableFieldlist=multiValueTableFieldsMap.get(tableFieldsPropertiesEntry.getKey());

        List<List<String>> tempTableFieldsHolder=new ArrayList<>();
        for (List<String> tableFields:existingTableFieldlist){
            if (!tableFields.equals(tableFieldsPropertiesEntry.getValue().fields())){
                tempTableFieldsHolder.add(tableFieldsPropertiesEntry.getValue().fields());
            }
        }
        if (!tempTableFieldsHolder.isEmpty()){
            existingTableFieldlist.addAll(tempTableFieldsHolder);
        }
    }

}
