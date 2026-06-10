package com.app.decisioniq.api;

import com.app.decisioniq.service.AgentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentApi {

    private AgentService agentService;

    public AgentApi(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/ask")
    public String processRequest(@RequestBody String question){
        return agentService.processQuestion(question);
    }
}
