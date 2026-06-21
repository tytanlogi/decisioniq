package com.app.decisioniq.assistant.dataslice.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML configuration for one reusable data slice.
 */
public record DataSliceProperties(String description, Map<String, TableFieldsProperties> tables) {

    public DataSliceProperties(String description, Map<String, TableFieldsProperties> tables) {
        this.description = description;
        this.tables = tables == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(tables));
    }

}
