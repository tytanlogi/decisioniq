package com.app.decisioniq.api;

import com.app.decisioniq.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dq")
public class ChatController {

    private ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    @PostMapping("/chat")
    public String chat(@RequestBody String request){
        return chatService.speakToLLMService(request);
    }



}
