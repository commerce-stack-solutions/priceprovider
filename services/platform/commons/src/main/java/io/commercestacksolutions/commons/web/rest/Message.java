package io.commercestacksolutions.commons.web.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class Message {

    public enum MessageType {
        INFO, ERROR, WARNING, DANGER, SUCCESS, DEBUG
    }

    @JsonProperty("type")
    private MessageType type;

    @JsonProperty("message-key")
    private String messageKey;

    @JsonProperty("parameters")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> parameters;

    @JsonProperty("status-code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer statusCode;

    @JsonProperty("fields")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> fields;

    // Constructor with message key and parameters
    public Message(MessageType type, String messageKey, Map<String, String> parameters, List<String> fields) {
        this.type = type;
        this.messageKey = messageKey;
        this.parameters = parameters;
        this.fields = fields;
    }

    // Constructor with message key, parameters and status code
    public Message(MessageType type, String messageKey, Map<String, String> parameters, int statusCode, List<String> fields) {
        this.type = type;
        this.messageKey = messageKey;
        this.parameters = parameters;
        this.statusCode = statusCode;
        this.fields = fields;
    }

    // Constructor with just message key (no parameters)
    public Message(MessageType type, String messageKey) {
        this.type = type;
        this.messageKey = messageKey;
    }

    // Constructor with message key and fields (no parameters)
    public Message(MessageType type, String messageKey, List<String> fields) {
        this.type = type;
        this.messageKey = messageKey;
        this.fields = fields;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }
}