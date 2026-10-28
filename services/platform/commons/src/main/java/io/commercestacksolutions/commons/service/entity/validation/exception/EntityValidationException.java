package io.commercestacksolutions.commons.service.entity.validation.exception;

import io.commercestacksolutions.commons.web.rest.ErrorResponse;
import io.commercestacksolutions.commons.web.rest.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when entity validation fails.
 * Contains the validation error messages that should be returned to the client.
 */
public class EntityValidationException extends Exception {

    private final ErrorResponse errorResponse;

    public EntityValidationException(String message, List<Message> validationMessages) {
        super(message);
        errorResponse = new ErrorResponse(validationMessages != null ? validationMessages : new ArrayList<>());
    }

    public EntityValidationException(String message, Message validationMessage) {
        super(message);
        errorResponse = new ErrorResponse();
        this.errorResponse.addMessage(validationMessage);
    }

    public List<Message> getMessages() {
        return this.errorResponse.getMessages();
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
