package com.app.decisioniq.assistant.dataslice.config;

import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML-backed configuration for reusable data slices.
 */
@Getter
@ConfigurationProperties(prefix = "decisioniq.assistant")
public class AssistantDataSliceProperties {

    private final Map<DecisionDataSlice, DataSliceProperties> dataSlices;

    public AssistantDataSliceProperties(Map<DecisionDataSlice, DataSliceProperties> dataSlices) {
        this.dataSlices = dataSlices == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(dataSlices));
    }

}
