package com.app.decisioniq.application.nlp;

import java.util.List;

public record NlpOperationFrame(
        int sentenceIndex,
        int clauseIndex,
        String text,
        List<String> actions,
        List<String> objects,
        List<String> targets,
        Effect effect
) {
    public NlpOperationFrame {
        actions = List.copyOf(actions);
        objects = List.copyOf(objects);
        targets = List.copyOf(targets);
    }

    public boolean blocksRequest() {
        return effect == Effect.PERSIST
                || effect == Effect.MODIFY
                || effect == Effect.TRANSFER
                || effect == Effect.EXTERNAL_ACTION;
    }

    public enum Effect {
        SAFE_CANDIDATE,
        PERSIST,
        MODIFY,
        TRANSFER,
        EXTERNAL_ACTION
    }
}
