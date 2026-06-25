package com.app.decisioniq.service.evidence;

import com.app.decisioniq.assistant.evidence.model.SQLTableData;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;


@Service
@Slf4j
public class SQLDataFetchService {


    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SQLDataFetchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SQLTableData buildQueryAndFetchData(QueryPlan query){
        return initiateDataScanFromMap(query);
    }

    private SQLTableData initiateDataScanFromMap(QueryPlan queryPlan){
        SQLTableData sqlTableData=new SQLTableData();
        Map<String,MultiValueMap<String, List<Map<String, Object>>>> intentTableMap=new HashMap<>();
        //every intent will be scanned and sent
        for (Map.Entry<String,MultiValueMap<String,List<String>>> sqlTableFieldValue:queryPlan.getSqlTableFieldsMap().entrySet()){
            MultiValueMap<String, List<Map<String, Object>>> finalDataFromTable = initiateQueryBuildingPerIntent(sqlTableFieldValue.getValue(), queryPlan);
            intentTableMap.put(sqlTableFieldValue.getKey(),finalDataFromTable);
        }
        sqlTableData.setIntentTableMap(intentTableMap);
        return sqlTableData;
    }

    private MultiValueMap<String, List<Map<String, Object>>> initiateQueryBuildingPerIntent(MultiValueMap<String,List<String>> tableToFieldsMap, QueryPlan queryPlan){

        MultiValueMap<String,List<Map<String,Object>>> multiValueTableDataMap=new LinkedMultiValueMap<>();
        List<List<Map<String, @Nullable Object>>> rowDataList=new LinkedList<>();
        for (Map.Entry<String,List<List<String>>> sqlTableFields:tableToFieldsMap.entrySet()){
            String tableName=sqlTableFields.getKey();
            List<List<String>> fieldsValueList = sqlTableFields.getValue();
            if (fieldsValueList.size()==1){
                String fields = String.join(",", fieldsValueList.get(0));
                List<Map<String, @Nullable Object>> maps = buildAndExecuteDynamicQueryPerTable(tableName, fields, queryPlan);
                insertDataToMultiMap(tableName,multiValueTableDataMap,buildAndExecuteDynamicQueryPerTable(tableName, fields, queryPlan));
            }else{
                for (List<String> fieldList:fieldsValueList){
                    String fields = String.join(",", fieldList);
                    insertDataToMultiMap(tableName,multiValueTableDataMap,buildAndExecuteDynamicQueryPerTable(tableName, fields, queryPlan));
                }
            }
        }
        return multiValueTableDataMap;
    }

    private List<Map<String, @Nullable Object>> buildAndExecuteDynamicQueryPerTable(String tableName, String fields, QueryPlan queryPlan){
        String sql = String.format("""
                        SELECT %s FROM %s
                        where transaction_id = ?
                        """,
                fields,tableName);
        log.info("Query:{}",sql);
        return jdbcTemplate.queryForList(sql,queryPlan.getTransactionId());
    }

    private void insertDataToMultiMap(String tableName, MultiValueMap<String,List<Map<String,Object>>> multiValueTableDataMap,
                                      List<Map<String, @Nullable Object>> tableData){
        if (multiValueTableDataMap.containsKey(tableName)){
            multiValueTableDataMap.get(tableName).add(tableData);
        }else {
            List<List<Map<String, @Nullable Object>>> newTableData=new LinkedList<>();
            newTableData.add(tableData);
            multiValueTableDataMap.put(tableName,newTableData);
        }
    }

}
