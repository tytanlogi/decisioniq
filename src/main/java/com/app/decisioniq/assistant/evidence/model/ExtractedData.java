package com.app.decisioniq.assistant.evidence.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExtractedData implements Serializable {

    private SQLTableData sqlTableData;
}
