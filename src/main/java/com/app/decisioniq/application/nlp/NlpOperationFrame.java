package com.app.decisioniq.application.nlp;

import java.util.List;

public record NlpOperationFrame(
        int sentenceIndex,
        int clauseIndex,
        String text,
        List<String> actions,
        List<String> objects,
        List<String> targets,
        Effect effect,
        NlpClauseRole role,
        NlpContextCandidate contextCandidate,
        String resolvedTransactionId
) {
    public NlpOperationFrame {
        actions = List.copyOf(actions);
        objects = List.copyOf(objects);
        targets = List.copyOf(targets);
        role = role == null ? NlpClauseRole.EXECUTABLE_REQUEST : role;
    }

    public NlpOperationFrame(
            int sentenceIndex,
            int clauseIndex,
            String text,
            List<String> actions,
            List<String> objects,
            List<String> targets,
            Effect effect
    ) {
        this(sentenceIndex, clauseIndex, text, actions, objects, targets, effect,
                NlpClauseRole.EXECUTABLE_REQUEST, null, null);
    }

    public boolean executable() {
        return role == NlpClauseRole.EXECUTABLE_REQUEST;
    }

    public NlpOperationFrame withRole(
            NlpClauseRole updatedRole,
            NlpContextCandidate updatedContextCandidate,
            String updatedResolvedTransactionId
    ) {
        return new NlpOperationFrame(
                sentenceIndex, clauseIndex, text, actions, objects, targets, effect,
                updatedRole, updatedContextCandidate, updatedResolvedTransactionId
        );
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
