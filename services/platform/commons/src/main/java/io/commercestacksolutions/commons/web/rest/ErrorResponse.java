package io.commercestacksolutions.commons.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard error response structure for REST APIs.
 * Contains a list of error messages that can be returned to the frontend.
 */
public class ErrorResponse {
    
    @JsonProperty("$messages")
    private List<Message> messages;
    
    public ErrorResponse() {
        this.messages = new ArrayList<>();
    }
    
    public ErrorResponse(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }
    
    public ErrorResponse(Message message) {
        this.messages = new ArrayList<>();
        this.messages.add(message);
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    public void addMessage(Message message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
}
