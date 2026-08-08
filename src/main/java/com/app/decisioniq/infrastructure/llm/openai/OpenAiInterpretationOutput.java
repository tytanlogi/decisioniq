package com.app.decisioniq.infrastructure.llm.openai;

import java.util.List;

public class OpenAiInterpretationOutput {
    public String disposition;
    public List<Unit> units;
    public List<String> clarificationQuestions;

    public static class Unit {
        public String sourceUnitId;
        public String disposition;
        public String operation;
        public List<String> selectedCatalogKeys;
        public String contextSourceUnitId;
    }
}
