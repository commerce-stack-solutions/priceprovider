package io.commercestacksolutions.commons.query.messagekeys;

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
    public static final String ERROR_MAPPING_INSTANTIATION = "common.errors.mapping.instantiation";
    public static final String ERROR_MAPPING_PATCH_OPERATION = "common.errors.mapping.patchOperation";
    public static final String ERROR_MAPPING_NO_TARGET_CLASS = "common.errors.mapping.noTargetClass";
    public static final String ERROR_APPLYING_PATCH = "common.errors.applyingPatch";

    private MessageKeys() {
        // utility
    }
}
