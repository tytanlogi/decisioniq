package com.app.decisioniq.application.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NlpAnalyzer {

    private final StanfordCoreNLP pipeline;

    public NlpAnalyzer(@Lazy StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }

    public NlpAnalysis analyze(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }

        List<NlpAnalysis.Sentence> sentences = new ArrayList<>();
        for (TextSegment segment : sentenceInputSegments(text)) {
            CoreDocument document = new CoreDocument(segment.text());
            synchronized (pipeline) {
                pipeline.annotate(document);
            }
            for (CoreSentence sentence : document.sentences()) {
                sentences.add(toSentence(
                        sentences.size(), sentence, text, segment.begin()
                ));
            }
        }
        return new NlpAnalysis(text, sentences);
    }

    private NlpAnalysis.Sentence toSentence(
            int index,
            CoreSentence sentence,
            String source,
            int sourceOffset
    ) {
        List<NlpAnalysis.Token> tokens = sentence.tokens().stream()
                .map(token -> toToken(token, sourceOffset))
                .toList();
        SemanticGraph graph = sentence.dependencyParse();
        List<NlpAnalysis.Dependency> dependencies = dependencies(graph);
        List<NlpAnalysis.Clause> clauses = clauses(
                sentence.tokens(), graph, source, sourceOffset
        );
        int begin = sourceOffset + sentence.coreMap().get(
                CoreAnnotations.CharacterOffsetBeginAnnotation.class
        );
        int end = sourceOffset + sentence.coreMap().get(
                CoreAnnotations.CharacterOffsetEndAnnotation.class
        );
        return new NlpAnalysis.Sentence(
                index,
                source.substring(begin, end),
                begin,
                end,
                tokens,
                dependencies,
                clauses
        );
    }

    private NlpAnalysis.Token toToken(CoreLabel token, int sourceOffset) {
        return new NlpAnalysis.Token(
                token.index(),
                token.originalText(),
                token.lemma(),
                token.tag(),
                sourceOffset + token.beginPosition(),
                sourceOffset + token.endPosition()
        );
    }

    private List<NlpAnalysis.Dependency> dependencies(SemanticGraph graph) {
        List<NlpAnalysis.Dependency> dependencies = new ArrayList<>();
        graph.getRoots().stream()
                .sorted(Comparator.comparingInt(IndexedWord::index))
                .forEach(root -> dependencies.add(new NlpAnalysis.Dependency("root", 0, root.index())));
        graph.edgeListSorted().forEach(edge -> dependencies.add(new NlpAnalysis.Dependency(
                edge.getRelation().toString(),
                edge.getGovernor().index(),
                edge.getDependent().index()
        )));
        return List.copyOf(dependencies);
    }

    private List<NlpAnalysis.Clause> clauses(
            List<CoreLabel> tokens,
            SemanticGraph graph,
            String source,
            int sourceOffset
    ) {
        if (tokens.isEmpty()) {
            return List.of();
        }

        Map<Integer, ClauseCut> cuts = new LinkedHashMap<>();
        for (CoreLabel token : tokens) {
            if (";".equals(token.originalText()) && token.index() < tokens.size()) {
                cuts.put(token.index(), new ClauseCut(token.index(), token.index() + 1));
            }
        }
        for (SemanticGraphEdge edge : graph.edgeListSorted()) {
            if (!"conj".equals(edge.getRelation().getShortName())
                    || !startsIndependentClause(
                    edge.getGovernor(), edge.getDependent(), graph, tokens
            )) {
                continue;
            }
            int conjunction = coordinatingTokenBetween(
                    tokens,
                    edge.getGovernor().index(),
                    edge.getDependent().index()
            );
            if (conjunction > 0) {
                cuts.put(conjunction, new ClauseCut(conjunction, conjunction + 1));
            }
        }
        for (SemanticGraphEdge edge : graph.edgeListSorted()) {
            if (!"mark".equals(edge.getRelation().getShortName())) {
                continue;
            }
            int conjunction = coordinatingTokenBefore(tokens, edge.getDependent().index());
            if (conjunction > 0) {
                cuts.put(conjunction, new ClauseCut(conjunction, conjunction + 1));
            }
        }

        List<ClauseCut> orderedCuts = cuts.values().stream()
                .sorted(Comparator.comparingInt(ClauseCut::separatorIndex))
                .toList();
        List<NlpAnalysis.Clause> clauses = new ArrayList<>();
        int first = tokens.getFirst().index();
        for (ClauseCut cut : orderedCuts) {
            addClause(
                    clauses, tokens, source, sourceOffset,
                    first, cut.separatorIndex() - 1
            );
            first = cut.nextTokenIndex();
        }
        addClause(
                clauses, tokens, source, sourceOffset,
                first, tokens.getLast().index()
        );
        return List.copyOf(clauses);
    }

    private boolean startsIndependentClause(
            IndexedWord governor,
            IndexedWord word,
            SemanticGraph graph,
            List<CoreLabel> tokens
    ) {
        String partOfSpeech = word.tag();
        if (partOfSpeech != null && partOfSpeech.startsWith("VB")) {
            return true;
        }
        boolean hasPredicateMarker = graph.outgoingEdgeList(word).stream()
                .map(edge -> edge.getRelation().getShortName())
                .anyMatch(relation -> "cop".equals(relation) || "aux".equals(relation));
        if (hasPredicateMarker) {
            return true;
        }
        if (governor.tag() != null
                && governor.tag().startsWith("VB")
                && partOfSpeech != null
                && partOfSpeech.startsWith("JJ")
                && hasFollowingNoun(word.index(), tokens)) {
            return true;
        }
        return governor.tag() != null
                && governor.tag().startsWith("VB")
                && partOfSpeech != null
                && partOfSpeech.startsWith("NN")
                && hasFollowingContent(word.index(), tokens);
    }

    private boolean hasFollowingNoun(int tokenIndex, List<CoreLabel> tokens) {
        return tokens.stream()
                .filter(token -> token.index() > tokenIndex)
                .anyMatch(token -> token.tag() != null && token.tag().startsWith("NN"));
    }

    private boolean hasFollowingContent(int tokenIndex, List<CoreLabel> tokens) {
        return tokens.stream()
                .filter(token -> token.index() > tokenIndex)
                .anyMatch(token -> !".".equals(token.tag()));
    }

    private int coordinatingTokenBetween(List<CoreLabel> tokens, int left, int right) {
        int start = Math.min(left, right);
        int end = Math.max(left, right);
        return tokens.stream()
                .filter(token -> token.index() > start && token.index() < end)
                .filter(token -> "CC".equals(token.tag()))
                .mapToInt(CoreLabel::index)
                .max()
                .orElse(-1);
    }

    private int coordinatingTokenBefore(List<CoreLabel> tokens, int tokenIndex) {
        return tokens.stream()
                .filter(token -> token.index() < tokenIndex)
                .filter(token -> "CC".equals(token.tag()))
                .mapToInt(CoreLabel::index)
                .max()
                .orElse(-1);
    }

    private void addClause(
            List<NlpAnalysis.Clause> clauses,
            List<CoreLabel> tokens,
            String source,
            int sourceOffset,
            int firstTokenIndex,
            int lastTokenIndex
    ) {
        if (firstTokenIndex > lastTokenIndex) {
            return;
        }
        CoreLabel first = token(tokens, firstTokenIndex);
        CoreLabel last = token(tokens, lastTokenIndex);
        int begin = sourceOffset + first.beginPosition();
        int end = sourceOffset + last.endPosition();
        clauses.add(new NlpAnalysis.Clause(
                clauses.size(),
                source.substring(begin, end).strip(),
                begin,
                end,
                firstTokenIndex,
                lastTokenIndex
        ));
    }

    private CoreLabel token(List<CoreLabel> tokens, int tokenIndex) {
        return tokens.get(tokenIndex - tokens.getFirst().index());
    }

    private List<TextSegment> sentenceInputSegments(String text) {
        List<TextSegment> segments = new ArrayList<>();
        int begin = 0;
        for (int index = 0; index < text.length() - 1; index++) {
            char current = text.charAt(index);
            char next = text.charAt(index + 1);
            if (!isSentencePunctuation(current)
                    || Character.isWhitespace(next)
                    || !Character.isUpperCase(next)) {
                continue;
            }
            segments.add(new TextSegment(begin, text.substring(begin, index + 1)));
            begin = index + 1;
        }
        if (begin < text.length()) {
            segments.add(new TextSegment(begin, text.substring(begin)));
        }
        return List.copyOf(segments);
    }

    private boolean isSentencePunctuation(char value) {
        return value == '.' || value == '?' || value == '!';
    }

    private record ClauseCut(int separatorIndex, int nextTokenIndex) { }

    private record TextSegment(int begin, String text) { }
}
