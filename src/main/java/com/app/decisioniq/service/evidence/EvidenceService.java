package com.app.decisioniq.service.evidence;

import com.app.decisioniq.assistant.planning.model.QueryPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class EvidenceService {

    public void initiateEvidenceCollectionMechanism(QueryPlan query) {
        log.info("Evidence collection Started");
    }
}
