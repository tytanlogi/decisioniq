package com.app.decisioniq.assistant.dataslice.config;

import java.util.Collections;
import java.util.List;

/**
 * YAML configuration for the fields required from one table.
 */
public record TableFieldsProperties(List<String> fields) {

    public TableFieldsProperties(List<String> fields) {
        this.fields = fields == null ? List.of() : Collections.unmodifiableList(fields);
    }

}
