package io.commercestacksolutions.commons.query;

import java.util.Objects;

/**
 * Represents a single filter condition in a query.
 * Supports field-based filtering with various operators.
 */
public class QueryFilter {
    
    private final String field;
    private final FilterOperator operator;
    private final Object value;
    
    public QueryFilter(String field, FilterOperator operator, Object value) {
        this.field = Objects.requireNonNull(field, "Field cannot be null");
        this.operator = Objects.requireNonNull(operator, "Operator cannot be null");
        this.value = value;
    }
    
    public String getField() {
        return field;
    }
    
    public FilterOperator getOperator() {
        return operator;
    }
    
    public Object getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return "QueryFilter{field='" + field + "', operator=" + operator + ", value=" + value + "}";
    }
    
    /**
     * Supported filter operators based on data types.
     */
    public enum FilterOperator {
        // String / Reference operations
        EQUALS,           // field:value
        CONTAINS,         // field:*value* (implicit)
        
        // Numeric / Date / DateTime operations
        GREATER_THAN,     // field:>value or field:[value TO *]
        LESS_THAN,        // field:<value or field:[* TO value]
        GREATER_THAN_OR_EQUAL, // field:>=value
        LESS_THAN_OR_EQUAL,    // field:<=value
        RANGE,            // field:[min TO max]
        
        // Boolean operations
        // EQUALS is reused for boolean
        
        // Existence checks (all types)
        EXISTS,           // field.exists:true
        NOT_EXISTS        // field.exists:false
    }
}
