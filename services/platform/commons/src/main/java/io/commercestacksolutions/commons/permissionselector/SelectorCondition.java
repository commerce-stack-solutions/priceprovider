package io.commercestacksolutions.commons.permissionselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single condition in a selector expression.
 *
 * <p>Examples:
 * <ul>
 *   <li>currencyRef == 'EUR'</li>
 *   <li>priceType != 'PURCHASE_PRICE'</li>
 *   <li>channelRefs hasAny('a','b','c')</li>
 *   <li>channelRefs hasAll('a','b')</li>
 *   <li>groupRefs isEmpty</li>
 * </ul>
 */
public class SelectorCondition {

    private final String field;
    private final SelectorOperator operator;
    private final List<String> values;

    /**
     * Creates a condition for operators that don't require values (IS_EMPTY).
     */
    public SelectorCondition(String field, SelectorOperator operator) {
        this(field, operator, Collections.emptyList());
    }

    /**
     * Creates a condition for operators with a single value (EQUALS, NOT_EQUALS).
     */
    public SelectorCondition(String field, SelectorOperator operator, String value) {
        this(field, operator, Collections.singletonList(value));
    }

    /**
     * Creates a condition for operators with multiple values (HAS_ANY, HAS_ALL).
     */
    public SelectorCondition(String field, SelectorOperator operator, List<String> values) {
        this.field = Objects.requireNonNull(field, "Field cannot be null");
        this.operator = Objects.requireNonNull(operator, "Operator cannot be null");
        this.values = new ArrayList<>(Objects.requireNonNull(values, "Values cannot be null"));

        // Validate that operator has appropriate number of values
        switch (operator) {
            case IS_EMPTY:
                if (!this.values.isEmpty()) {
                    throw new IllegalArgumentException("IS_EMPTY operator does not accept values");
                }
                break;
            case EQUALS:
            case NOT_EQUALS:
                if (this.values.size() != 1) {
                    throw new IllegalArgumentException(operator + " operator requires exactly one value");
                }
                break;
            case HAS_ANY:
            case HAS_ALL:
                if (this.values.isEmpty()) {
                    throw new IllegalArgumentException(operator + " operator requires at least one value");
                }
                break;
        }
    }

    public String getField() {
        return field;
    }

    public SelectorOperator getOperator() {
        return operator;
    }

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Returns the single value for operators like EQUALS/NOT_EQUALS.
     */
    public String getValue() {
        if (values.size() != 1) {
            throw new IllegalStateException("getValue() only valid for single-value operators");
        }
        return values.get(0);
    }

    @Override
    public String toString() {
        switch (operator) {
            case IS_EMPTY:
                return field + " isEmpty";
            case EQUALS:
                return field + " == '" + getValue() + "'";
            case NOT_EQUALS:
                return field + " != '" + getValue() + "'";
            case HAS_ANY:
                return field + " hasAny(" + formatValueList() + ")";
            case HAS_ALL:
                return field + " hasAll(" + formatValueList() + ")";
            default:
                return field + " " + operator + " " + values;
        }
    }

    private String formatValueList() {
        return String.join(",", values.stream()
                .map(v -> "'" + v + "'")
                .toArray(String[]::new));
    }
}
