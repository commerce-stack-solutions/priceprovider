package io.commercestacksolutions.commons.query;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

/**
 * Runtime exception wrapper for query filtering errors.
 * Used to wrap checked InvalidParameterException in JPA Specification lambdas.
 * This exception should be caught and unwrapped by the service layer.
 */
public class QueryFilterRuntimeException extends RuntimeException {
    
    private final InvalidParameterException invalidParameterException;
    
    public QueryFilterRuntimeException(InvalidParameterException cause) {
        super(cause.getMessage(), cause);
        this.invalidParameterException = cause;
    }
    
    public InvalidParameterException getInvalidParameterException() {
        return invalidParameterException;
    }
}
