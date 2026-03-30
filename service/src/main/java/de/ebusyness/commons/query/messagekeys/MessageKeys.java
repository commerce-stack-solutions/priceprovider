package de.ebusyness.commons.query.messagekeys;

/**
 * Message key constants for query parsing errors.
 */
public final class MessageKeys {

    public static final String ERROR_QUERY_SYNTAX = "common.errors.query.syntax";
    public static final String ERROR_QUERY_NESTING_DEPTH = "common.errors.query.nestingDepth";
    public static final String ERROR_QUERY_MISSING_CLOSING_PAREN = "common.errors.query.missingClosingParenthesis";
    public static final String ERROR_QUERY_INVALID_FILTER = "common.errors.query.invalidFilter";
    public static final String ERROR_QUERY_INVALID_OPERATOR = "common.errors.query.invalidOperator";
    public static final String ERROR_QUERY_INVALID_COLLECTION_OPERATOR = "common.errors.query.invalidCollectionOperator";
    public static final String ERROR_QUERY_INVALID_VALUE_TYPE = "common.errors.query.invalidValueType";

    private MessageKeys() {
        // utility
    }
}
