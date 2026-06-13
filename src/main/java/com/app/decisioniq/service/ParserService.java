package com.app.decisioniq.service;

import com.app.decisioniq.llm.DecisionIqIntent;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ParserService {

    public void parserIncomingReq(String req) {
        Set<String> tokensSet = performTokenization(req);
        List<String> intentList = filterIntentBasedOnTokens(tokensSet);
        System.out.println(intentList);
    }

    private Set<String> performTokenization(String req) {
        Set<String> tokenSet=new HashSet<>();
        try (Analyzer analyzer = new StandardAnalyzer()) {
            TokenStream tokenStream = analyzer.tokenStream("field", req);
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                String token=charTermAttribute.toString();
                if (EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(token)) {
                    continue;
                }
                tokenSet.add(charTermAttribute.toString().toUpperCase());
            }
            tokenStream.close();
            analyzer.close();
            return tokenSet;
        } catch (Exception e) {
            log.error("Exception caught while tokenizing");
        }
        return null;
    }

    private List<String> filterIntentBasedOnTokens(Set<String> tokenSet) {
        List<String> intentList = new ArrayList<>();
        for (DecisionIqIntent decisionIqIntent : DecisionIqIntent.values()) {
            for (String token:tokenSet){
                if (decisionIqIntent.name().contains(token)){
                    intentList.add(decisionIqIntent.name());
                }
            }
        }
        return intentList;
    }

}
