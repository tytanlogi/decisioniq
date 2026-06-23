package com.app.decisioniq.assistant.planning.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * RAG-side evidence requirements for a getQuery plan.
 */
@Data
public class RagData implements Serializable {

    private String collection;
    private List<String> chunkTypes;
    private Integer topK;
}
