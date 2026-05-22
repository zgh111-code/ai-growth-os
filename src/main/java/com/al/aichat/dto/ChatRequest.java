package com.al.aichat.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer maxTokens;

    public ChatRequest() {}

    public ChatRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
