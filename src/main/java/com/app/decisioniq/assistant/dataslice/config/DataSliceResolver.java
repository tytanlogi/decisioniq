package com.app.decisioniq.assistant.dataslice.config;

import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import org.springframework.stereotype.Component;

/**
 * Resolves one reusable data-slice definition from YAML configuration.
 */
@Component
public class DataSliceResolver {

    private final AssistantDataSliceProperties properties;

    public DataSliceResolver(AssistantDataSliceProperties properties) {
        this.properties = properties;
    }

    public DataSliceProperties resolve(DecisionDataSlice dataSlice) {
        DataSliceProperties configuredSlice = properties.getDataSlices().get(dataSlice);
        if (configuredSlice == null) {
            throw new IllegalArgumentException("No YAML data-slice configuration found for " + dataSlice);
        }
        return configuredSlice;
    }
}
