package io.commercestacksolutions.commons.web.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for creating Message objects with common patterns.
 */
public class MessageBuilder {
    
    /**
     * Creates a simple message with just a message key.
     */
    public static Message create(Message.MessageType type, String messageKey) {
        return new Message(type, messageKey);
    }
    
    /**
     * Creates a message with a message key and fields.
     */
    public static Message create(Message.MessageType type, String messageKey, List<String> fields) {
        return new Message(type, messageKey, fields);
    }
    
    /**
     * Creates a message with a message key and one parameter.
     */
    public static Message create(Message.MessageType type, String messageKey, String paramKey, String paramValue) {
        Map<String, String> params = new HashMap<>();
        params.put(paramKey, paramValue);
        return new Message(type, messageKey, params, null);
    }
    
    /**
     * Creates a message with a message key, one parameter, and fields.
     */
    public static Message create(Message.MessageType type, String messageKey, String paramKey, String paramValue, List<String> fields) {
        Map<String, String> params = new HashMap<>();
        params.put(paramKey, paramValue);
        return new Message(type, messageKey, params, fields);
    }
    
    /**
     * Creates a message with a message key and two parameters.
     */
    public static Message create(Message.MessageType type, String messageKey, 
                                 String paramKey1, String paramValue1,
                                 String paramKey2, String paramValue2) {
        Map<String, String> params = new HashMap<>();
        params.put(paramKey1, paramValue1);
        params.put(paramKey2, paramValue2);
        return new Message(type, messageKey, params, null);
    }
    
    /**
     * Creates a message with a message key, parameters map, and fields.
     */
    public static Message create(Message.MessageType type, String messageKey, 
                                 Map<String, String> parameters, List<String> fields) {
        return new Message(type, messageKey, parameters, fields);
    }
    
    private MessageBuilder() {
        // Utility class
    }
}
