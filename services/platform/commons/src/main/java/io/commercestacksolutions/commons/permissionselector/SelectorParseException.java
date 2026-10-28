package io.commercestacksolutions.commons.permissionselector;

/**
 * Exception thrown when parsing a selector string fails.
 */
public class SelectorParseException extends RuntimeException {

    public SelectorParseException(String message) {
        super(message);
    }

    public SelectorParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
