package io.commercestacksolutions.commons.web.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestEntity<INFO_TYPE,INCLUDES_TYPE> {

    @JsonProperty("$info")
    private INFO_TYPE info;

    @JsonProperty("$includes")
    private INCLUDES_TYPE includes;

    @JsonProperty("$meta")
    private MetaInfo meta;

    @JsonProperty("$messages")
    private List<Message> messages;

    public INFO_TYPE getInfo() {
        return info;
    }

    public void setInfo(INFO_TYPE info) {
        this.info = info;
    }

    public INCLUDES_TYPE getIncludes() {
        return includes;
    }

    public void setIncludes(INCLUDES_TYPE includes) {
        this.includes = includes;
    }

    public MetaInfo getMeta() {
        return meta;
    }

    public void setMeta(MetaInfo meta) {
        this.meta = meta;
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