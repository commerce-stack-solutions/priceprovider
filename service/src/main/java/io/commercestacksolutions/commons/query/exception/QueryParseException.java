package io.commercestacksolutions.commons.query.exception;

import io.commercestacksolutions.commons.web.rest.ErrorResponse;
import io.commercestacksolutions.commons.web.rest.Message;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exception thrown when query parsing fails.
 */
public class QueryParseException extends Exception {
    private final ErrorResponse errorResponse;

    // Constructor with message key only (no parameters, no throwable)
    public QueryParseException(String messageKey) {
        super(messageKey);
        this.errorResponse = new ErrorResponse(new Message(Message.MessageType.ERROR, messageKey, Collections.emptyMap(), null));
    }

    // Constructor with message key and throwable (no parameters)
    public QueryParseException(String messageKey, Throwable ex) {
        super(messageKey, ex);
        this.errorResponse = new ErrorResponse(new Message(Message.MessageType.ERROR, messageKey, Collections.emptyMap(), null));
    }

    // Constructor with message key, parameters and throwable
    public QueryParseException(String messageKey, Map<String, String> parameters, Throwable ex) {
        super(messageKey, ex);
        this.errorResponse = new ErrorResponse(new Message(Message.MessageType.ERROR, messageKey, parameters, null));
    }

    // Constructor with message key and parameters
    public QueryParseException(String messageKey, Map<String, String> parameters) {
        super(messageKey);
        this.errorResponse = new ErrorResponse(new Message(Message.MessageType.ERROR, messageKey, parameters, null));
    }

    // Constructor with ErrorResponse and throwable
    public QueryParseException(String messageKey, Throwable ex, ErrorResponse errorResponse) {
        super(messageKey, ex);
        this.errorResponse = errorResponse;
    }

    // Constructor with ErrorResponse directly
    public QueryParseException(String messageKey, ErrorResponse errorResponse) {
        super(messageKey);
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public List<Message> getMessages() {
        return this.errorResponse.getMessages();
    }
}
