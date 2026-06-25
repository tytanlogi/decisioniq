package com.app.decisioniq.assistant.evidence.model;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class SQLTableData implements Serializable {

    Map<String, MultiValueMap<String, List<Map<String, Object>>>> intentTableMap;
}
