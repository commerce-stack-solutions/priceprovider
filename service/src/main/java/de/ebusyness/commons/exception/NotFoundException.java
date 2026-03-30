package de.ebusyness.commons.exception;

import de.ebusyness.commons.web.rest.ErrorResponse;
import de.ebusyness.commons.web.rest.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exception thrown when a requested entity is not found.
 * This is a checked exception that should result in HTTP 404 Not Found.
 */
public class NotFoundException extends Exception {
    
    private final ErrorResponse errorResponse;
    
    // Constructor with message key only (no parameters)
    public NotFoundException(String messageKey) {
        super(messageKey);
        this.errorResponse = new ErrorResponse(new Message(Message.MessageType.ERROR, messageKey, Collections.emptyMap(), null));
    }
    
    // Constructor with message key and fields (no parameters)
    public NotFoundException(String messageKey, List<String> fields) {
        super(messageKey);
        this.errorResponse = new ErrorResponse(new Message(Message.MessageType.ERROR, messageKey, Collections.emptyMap(), fields));
    }
    
    // Constructor with message key and parameters
    public NotFoundException(String messageKey, Map<String, String> parameters) {
        super(messageKey);
        this.errorResponse = new ErrorResponse(new Message(Message.MessageType.ERROR, messageKey, parameters, null));
    }
    
    // Constructor with message key, parameters and fields
    public NotFoundException(String messageKey, Map<String, String> parameters, List<String> fields) {
        super(messageKey);
        this.errorResponse = new ErrorResponse(new Message(Message.MessageType.ERROR, messageKey, parameters, fields));
    }
    
    // Constructor with ErrorResponse directly
    public NotFoundException(String messageKey, ErrorResponse errorResponse) {
        super(messageKey);
        this.errorResponse = errorResponse;
    }
    
    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
    
    public List<Message> getMessages() {
        return errorResponse != null ? errorResponse.getMessages() : new ArrayList<>();
    }
}
