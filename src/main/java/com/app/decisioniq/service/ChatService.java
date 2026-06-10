package com.app.decisioniq.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final OllamaChatModel ollamaChatModel;
    private ParserService parserService;

    @Autowired
    public ChatService(OllamaChatModel ollamaChatModel, ParserService parserService) {
        this.ollamaChatModel = ollamaChatModel;
        this.parserService = parserService;
    }

    public String speakToLLMService(String chat){
        parserService.parserIncomingReq(chat);
        //return ollamaChatModel.chat(chat);
        return null;
    }
}
