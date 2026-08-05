package com.app.decisioniq.config.nlp;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Properties;

@Configuration
public class NlpConfiguration {

    @Bean
    @Lazy
    public StanfordCoreNLP stanfordCoreNlp() {
        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse");
        properties.setProperty("tokenize.options", "ptb3Escaping=false");
        return new StanfordCoreNLP(properties);
    }
}
