package com.app.decisioniq.application.nlp;

import java.util.List;

public record NlpAnalysis(String text, List<Sentence> sentences) {

    public NlpAnalysis {
        sentences = List.copyOf(sentences);
    }

    public record Sentence(
            int index,
            String text,
            int begin,
            int end,
            List<Token> tokens,
            List<Dependency> dependencies,
            List<Clause> clauses
    ) {
        public Sentence {
            tokens = List.copyOf(tokens);
            dependencies = List.copyOf(dependencies);
            clauses = List.copyOf(clauses);
        }
    }

    public record Token(
            int index,
            String text,
            String lemma,
            String partOfSpeech,
            int begin,
            int end
    ) { }

    public record Dependency(String relation, int governorIndex, int dependentIndex) { }

    public record Clause(
            int index,
            String text,
            int begin,
            int end,
            int firstTokenIndex,
            int lastTokenIndex
    ) { }
}
