package com.app.decisioniq.assistant.intent.model;

import com.app.decisioniq.assistant.intent.type.DecisionAssumption;
import com.app.decisioniq.assistant.intent.type.ParsedQuestionStatus;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ParsedQuestion implements Serializable {
    ParsedQuestionStatus status;
    String transactionId;
    List<ParsedAsk> asks;
    DecisionAssumption decisionAssumption;
    boolean clarificationRequired;
    boolean cud;
    String clarificationQuestion;
}

