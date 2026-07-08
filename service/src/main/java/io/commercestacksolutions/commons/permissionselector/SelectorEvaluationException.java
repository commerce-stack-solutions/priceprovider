package io.commercestacksolutions.commons.permissionselector;

/**
 * Exception thrown when evaluating a selector expression fails.
 */
public class SelectorEvaluationException extends RuntimeException {

    public SelectorEvaluationException(String message) {
        super(message);
    }

    public SelectorEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
