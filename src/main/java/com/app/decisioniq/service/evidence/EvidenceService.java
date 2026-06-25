package com.app.decisioniq.service.evidence;

import com.app.decisioniq.assistant.evidence.model.ExtractedData;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EvidenceService {

    private final SQLDataFetchService sqlDataFetchService;
    private final RAGDataFetchService ragDataFetchService;

    @Autowired
    public EvidenceService(SQLDataFetchService sqlDataFetchService, RAGDataFetchService ragDataFetchService) {
        this.sqlDataFetchService = sqlDataFetchService;
        this.ragDataFetchService = ragDataFetchService;
    }

    public ExtractedData initiateEvidenceCollectionMechanism(QueryPlan query) {
        log.info("Evidence collection Started");
        ExtractedData extractedData=new ExtractedData();
        extractedData.setSqlTableData(sqlDataFetchService.buildQueryAndFetchData(query));
        return extractedData;
    }

}
