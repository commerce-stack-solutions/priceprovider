package io.commercestacksolutions.commons.permissionselector;

/**
 * Supported operators in permission selector expressions.
 */
public enum SelectorOperator {
    /**
     * Equality check: field == 'value'
     */
    EQUALS,

    /**
     * Inequality check: field != 'value'
     */
    NOT_EQUALS,

    /**
     * Check if field contains any of the specified values: field hasAny('a','b','c')
     * Valid for: String, Enum, Referenced Entity, Referenced Entity Collection
     */
    HAS_ANY,

    /**
     * Check if field contains all of the specified values: field hasAll('a','b','c')
     * Valid for: Referenced Entity Collection
     */
    HAS_ALL,

    /**
     * Check if field is empty/null/unset: field isEmpty
     * Valid for: String, Referenced Entity, Referenced Entity Collection
     */
    IS_EMPTY
}
