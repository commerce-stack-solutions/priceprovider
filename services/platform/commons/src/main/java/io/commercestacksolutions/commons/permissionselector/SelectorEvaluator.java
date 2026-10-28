package io.commercestacksolutions.commons.permissionselector;

import io.commercestacksolutions.commons.dataaccess.ReferenceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluates a selector expression against an object instance.
 *
 * <p>Supports evaluation of:
 * <ul>
 *   <li>String fields</li>
 *   <li>Boolean fields</li>
 *   <li>Enum fields (compared by String value)</li>
 *   <li>Referenced entities (single) - compared by @ReferenceKey or @Id</li>
 *   <li>Referenced entity collections - compared by @ReferenceKey or @Id of elements</li>
 * </ul>
 *
 * <p>Thread-safe and reusable for multiple evaluations.
 */
public class SelectorEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(SelectorEvaluator.class);

    /**
     * Evaluates a selector expression against an object.
     *
     * @param expression the parsed selector expression
     * @param target     the object to evaluate against
     * @return true if the expression matches the object, false otherwise
     */
    public boolean evaluate(SelectorExpression expression, Object target) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }

        boolean result = evaluateExpression(expression, target);

        if (logger.isDebugEnabled()) {
            logger.debug("Evaluated selector '{}' against {}: {}", expression, target.getClass().getSimpleName(), result);
        }

        return result;
    }

    private boolean evaluateExpression(SelectorExpression expression, Object target) {
        boolean result;

        if (expression.isLeaf()) {
            result = evaluateCondition(expression.getCondition(), target);
        } else {
            // Composite expression - evaluate children
            List<Boolean> childResults = expression.getChildren().stream()
                    .map(child -> evaluateExpression(child, target))
                    .collect(Collectors.toList());

            // Apply logical operator
            if (expression.getLogicalOperator() == SelectorExpression.LogicalOperator.AND) {
                result = childResults.stream().allMatch(Boolean::booleanValue);
            } else { // OR
                result = childResults.stream().anyMatch(Boolean::booleanValue);
            }
        }

        // Apply negation if present
        return expression.isNegated() ? !result : result;
    }

    private boolean evaluateCondition(SelectorCondition condition, Object target) {
        String fieldName = condition.getField();
        Object fieldValue = getFieldValue(target, fieldName);

        switch (condition.getOperator()) {
            case EQUALS:
                return evaluateEquals(fieldValue, condition.getValue());
            case NOT_EQUALS:
                return !evaluateEquals(fieldValue, condition.getValue());
            case HAS_ANY:
                return evaluateHasAny(fieldValue, condition.getValues());
            case HAS_ALL:
                return evaluateHasAll(fieldValue, condition.getValues());
            case IS_EMPTY:
                return evaluateIsEmpty(fieldValue);
            default:
                throw new SelectorEvaluationException("Unsupported operator: " + condition.getOperator());
        }
    }

    private boolean evaluateEquals(Object fieldValue, String expectedValue) {
        String actualValue = convertToComparisonString(fieldValue);
        String expected = normalizeString(expectedValue);
        return Objects.equals(actualValue, expected);
    }

    private boolean evaluateHasAny(Object fieldValue, List<String> expectedValues) {
        if (fieldValue == null) {
            return false;
        }

        Set<String> normalizedExpected = expectedValues.stream()
                .map(this::normalizeString)
                .collect(Collectors.toSet());

        // Handle collections (e.g., Set<ChannelEntity>)
        if (fieldValue instanceof Collection) {
            Collection<?> collection = (Collection<?>) fieldValue;
            if (collection.isEmpty()) {
                return false;
            }

            Set<String> actualValues = collection.stream()
                    .map(this::convertToComparisonString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // At least one match
            return actualValues.stream().anyMatch(normalizedExpected::contains);
        }

        // Handle single value (e.g., String, Enum, Entity)
        String actualValue = convertToComparisonString(fieldValue);
        return actualValue != null && normalizedExpected.contains(actualValue);
    }

    private boolean evaluateHasAll(Object fieldValue, List<String> expectedValues) {
        if (fieldValue == null) {
            return false;
        }

        Set<String> normalizedExpected = expectedValues.stream()
                .map(this::normalizeString)
                .collect(Collectors.toSet());

        // Handle collections (e.g., Set<ChannelEntity>)
        if (fieldValue instanceof Collection) {
            Collection<?> collection = (Collection<?>) fieldValue;
            if (collection.isEmpty() && !expectedValues.isEmpty()) {
                return false;
            }

            Set<String> actualValues = collection.stream()
                    .map(this::convertToComparisonString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // All expected values must be present
            return actualValues.containsAll(normalizedExpected);
        }

        // For single values, hasAll behaves like equals for single expected value
        if (expectedValues.size() == 1) {
            String actualValue = convertToComparisonString(fieldValue);
            return actualValue != null && normalizedExpected.contains(actualValue);
        }

        // Cannot have all of multiple values in a single-value field
        return false;
    }

    private boolean evaluateIsEmpty(Object fieldValue) {
        if (fieldValue == null) {
            return true;
        }

        // Empty string
        if (fieldValue instanceof String) {
            return ((String) fieldValue).trim().isEmpty();
        }

        // Empty collection
        if (fieldValue instanceof Collection) {
            return ((Collection<?>) fieldValue).isEmpty();
        }

        // Any other non-null value is considered non-empty
        return false;
    }

    /**
     * Converts a field value to a normalized string for comparison.
     * Handles String, Enum, Entity (via @ReferenceKey or @Id), and null.
     */
    private String convertToComparisonString(Object value) {
        if (value == null) {
            return null;
        }

        // String: normalize
        if (value instanceof String) {
            return normalizeString((String) value);
        }

        // Enum: use string value
        if (value instanceof Enum) {
            return normalizeString(((Enum<?>) value).name());
        }

        // Entity: extract @ReferenceKey or @Id
        String refKey = extractReferenceKey(value);
        if (refKey != null) {
            return normalizeString(refKey);
        }

        String id = extractId(value);
        if (id != null) {
            return normalizeString(id);
        }

        // Fallback: toString
        return normalizeString(value.toString());
    }

    /**
     * Normalizes a string for case-sensitive exact matching.
     * Per spec: empty string and null are treated the same (both empty).
     */
    private String normalizeString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return value;
    }

    /**
     * Gets the value of a field from the target object using reflection.
     */
    private Object getFieldValue(Object target, String fieldName) {
        try {
            // Try getter method first (e.g., getChannelRefs, getChannels)
            String getterName = "get" + capitalize(fieldName);
            Method getter = findMethod(target.getClass(), getterName);
            if (getter != null) {
                getter.setAccessible(true);
                return getter.invoke(target);
            }

            // Try direct field access
            Field field = findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(target);
            }

            throw new SelectorEvaluationException("Field '" + fieldName + "' not found on " + target.getClass().getSimpleName());
        } catch (SelectorEvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new SelectorEvaluationException("Failed to access field '" + fieldName + "' on " + target.getClass().getSimpleName(), e);
        }
    }

    /**
     * Extracts the @ReferenceKey value from an entity.
     */
    private String extractReferenceKey(Object entity) {
        try {
            for (Field field : getAllFields(entity.getClass())) {
                if (field.isAnnotationPresent(ReferenceKey.class)) {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    return value != null ? value.toString() : null;
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract @ReferenceKey from {}: {}", entity.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    /**
     * Extracts the @Id value from an entity.
     */
    private String extractId(Object entity) {
        try {
            for (Field field : getAllFields(entity.getClass())) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    return value != null ? value.toString() : null;
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract @Id from {}: {}", entity.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        for (Field field : getAllFields(clazz)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
