package de.ebusyness.commons.query;

import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.messagekeys.MessageKeys;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.query.sqm.PathElementException;
import org.hibernate.query.sqm.TerminalPathException;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds JPA Specifications from QueryExpression objects.
 * Converts parsed query expressions into JPA Criteria API predicates.
 */
public class SpecificationBuilder {

    /**
     * Builds a JPA Specification from a QueryExpression.
     *
     * @param expression the parsed query expression
     * @param <T>        the entity type
     * @return a JPA Specification
     * @throws InvalidParameterException if the query contains invalid fields
     */
    public static <T> Specification<T> build(QueryExpression expression) throws InvalidParameterException {
        if (expression == null) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {
            try {
                return buildPredicate(expression, root, query, criteriaBuilder);
            } catch (QueryFilterRuntimeException e) {
                // Re-throw as is - will be caught by service layer
                throw e;
            } catch (Exception e) {
                // Wrap any unexpected exceptions
                throw new RuntimeException("Error building query predicate", e);
            }
        };
    }

    /**
     * Recursively builds a Predicate from a QueryExpression.
     */
    private static <T> Predicate buildPredicate(
            QueryExpression expression,
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder) {

        if (expression.isLeaf()) {
            Predicate predicate = buildFilterPredicate(expression.getFilter(), root, criteriaBuilder);
            return expression.isNegated() ? criteriaBuilder.not(predicate) : predicate;
        }

        // Composite expression - combine children with logical operator
        List<Predicate> childPredicates = new ArrayList<>();
        for (QueryExpression child : expression.getChildren()) {
            Predicate childPredicate = buildPredicate(child, root, query, criteriaBuilder);
            if (childPredicate != null) {
                childPredicates.add(childPredicate);
            }
        }

        if (childPredicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        Predicate combined;
        if (expression.getLogicalOperator() == QueryExpression.LogicalOperator.AND) {
            combined = criteriaBuilder.and(childPredicates.toArray(new Predicate[0]));
        } else {
            combined = criteriaBuilder.or(childPredicates.toArray(new Predicate[0]));
        }

        return expression.isNegated() ? criteriaBuilder.not(combined) : combined;
    }

    /**
     * Builds a Predicate from a single QueryFilter.
     */
    @SuppressWarnings("unchecked")
    private static <T> Predicate buildFilterPredicate(
            QueryFilter filter,
            Root<T> root,
            CriteriaBuilder criteriaBuilder) {

        String fieldPath = filter.getField();
        Path<?> path;

        try {
            path = resolvePath(root, fieldPath);
        } catch (PathElementException e) {
            List<String> fields = new ArrayList<>();
            fields.add(fieldPath);
            throw new QueryFilterRuntimeException(new InvalidParameterException("common.errors.query.fieldUnknown", fields));
        } catch (TerminalPathException e) {
            List<String> fields = new ArrayList<>();
            fields.add(fieldPath);
            throw new QueryFilterRuntimeException(new InvalidParameterException("common.errors.query.fieldInvalid", fields));
        } catch (IllegalArgumentException e) {
            List<String> fields = new ArrayList<>();
            fields.add(fieldPath);
            throw new QueryFilterRuntimeException(new InvalidParameterException("common.errors.query.fieldInvalid", fields));
        }

        switch (filter.getOperator()) {
            case EQUALS:
                return buildEqualsPredicate(path, filter.getValue(), criteriaBuilder);

            case CONTAINS:
                if (path.getJavaType() == String.class) {
                    return criteriaBuilder.like(
                            criteriaBuilder.lower((Path<String>) path),
                            "%" + filter.getValue().toString().toLowerCase() + "%"
                    );
                }
                // Handle enum types with substring matching on enum name
                if (path.getJavaType().isEnum()) {
                    return buildEnumSubstringPredicate(path, filter.getValue().toString(), criteriaBuilder);
                }
                return criteriaBuilder.equal(path, filter.getValue());

            case GREATER_THAN:
                validateNotCollection(path, fieldPath, ">");
                return buildComparisonPredicate(path, filter.getValue(), criteriaBuilder, ">");

            case LESS_THAN:
                validateNotCollection(path, fieldPath, "<");
                return buildComparisonPredicate(path, filter.getValue(), criteriaBuilder, "<");

            case GREATER_THAN_OR_EQUAL:
                validateNotCollection(path, fieldPath, ">=");
                return buildComparisonPredicate(path, filter.getValue(), criteriaBuilder, ">=");

            case LESS_THAN_OR_EQUAL:
                validateNotCollection(path, fieldPath, "<=");
                return buildComparisonPredicate(path, filter.getValue(), criteriaBuilder, "<=");

            case RANGE:
                validateNotCollection(path, fieldPath, "[min TO max]");
                Object[] range = (Object[]) filter.getValue();
                return buildRangePredicate(path, range[0], range[1], criteriaBuilder);

            case EXISTS:
                return buildExistsPredicate(path, criteriaBuilder, false);

            case NOT_EXISTS:
                return buildExistsPredicate(path, criteriaBuilder, true);

            default:
                throw new IllegalArgumentException("Unsupported operator: " + filter.getOperator());
        }
    }

    /**
     * Resolves a field path that may contain dots (e.g., "baseUnit.symbol").
     *
     * @throws IllegalArgumentException if any part of the path cannot be resolved
     */
    private static <T> Path<?> resolvePath(Root<T> root, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Path<?> path = root;

        for (String part : parts) {
            try {
                path = path.get(part);
            } catch (IllegalArgumentException e) {
                // Re-throw with more context
                throw new IllegalArgumentException("Could not resolve field '" + part + "' in path '" + fieldPath + "': " + e.getMessage(), e);
            }
        }

        return path;
    }

    /**
     * Builds an equals predicate with type-specific handling.
     */
    @SuppressWarnings("unchecked")
    private static Predicate buildEqualsPredicate(Path<?> path, Object value, CriteriaBuilder criteriaBuilder) {
        if (value == null) {
            return criteriaBuilder.isNull(path);
        }

        // If the path references an entity (association) but the provided value is a string,
        // try to compare against the referenced entity's id attribute (common case: currency:EUR)
        if (value instanceof String && path.getJavaType() != String.class) {
            String idAttr = QueryReflectionUtil.findIdAttributeName(path.getJavaType());
            if (idAttr != null) {
                try {
                    Path<?> idPath = path.get(idAttr);
                    Object converted = QueryReflectionUtil.convertValueToType(value, idPath.getJavaType());
                    return criteriaBuilder.equal(idPath, converted);
                } catch (IllegalArgumentException e) {
                    // Fall through to default equality if we cannot access id subpath
                }
            }
        }

        // For enums, try to match the exact enum constant by name
        if (value instanceof String && path.getJavaType().isEnum()) {
            String enumName = ((String) value).toUpperCase();
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumClass = (Class<? extends Enum>) path.getJavaType();
            
            // Try to find exact enum match
            for (Enum<?> enumValue : enumClass.getEnumConstants()) {
                if (enumValue.name().equals(enumName)) {
                    return criteriaBuilder.equal(path, enumValue);
                }
            }
            // No exact match found, return always-false predicate
            return criteriaBuilder.disjunction();
        }

        // For strings, use case-insensitive comparison if the value doesn't look like an ID
        if (path.getJavaType() == String.class && value instanceof String) {
            String strValue = (String) value;
            // If it's a short string (likely an ID/code), use exact match
            // Otherwise use case-insensitive contains
            if (strValue.length() <= 10) {
                return criteriaBuilder.equal(path, value);
            } else {
                return criteriaBuilder.like(
                        criteriaBuilder.lower((Path<String>) path),
                        "%" + strValue.toLowerCase() + "%"
                );
            }
        }

        return criteriaBuilder.equal(path, value);
    }


    /**
     * Builds a comparison predicate (>, <, >=, <=).
     */
    @SuppressWarnings("unchecked")
    private static Predicate buildComparisonPredicate(
            Path<?> path,
            Object value,
            CriteriaBuilder criteriaBuilder,
            String operator) {

        if (value == null) {
            return criteriaBuilder.conjunction();
        }

        // Handle different comparable types
        if (value instanceof Number) {
            Path<Number> numberPath = (Path<Number>) path;
            Number numberValue = (Number) value;

            switch (operator) {
                case ">":
                    return criteriaBuilder.gt(numberPath, numberValue);
                case "<":
                    return criteriaBuilder.lt(numberPath, numberValue);
                case ">=":
                    return criteriaBuilder.ge(numberPath, numberValue);
                case "<=":
                    return criteriaBuilder.le(numberPath, numberValue);
            }
        } else if (value instanceof OffsetDateTime) {
            Path<OffsetDateTime> datePath = (Path<OffsetDateTime>) path;
            OffsetDateTime dateValue = (OffsetDateTime) value;

            switch (operator) {
                case ">":
                    return criteriaBuilder.greaterThan(datePath, dateValue);
                case "<":
                    return criteriaBuilder.lessThan(datePath, dateValue);
                case ">=":
                    return criteriaBuilder.greaterThanOrEqualTo(datePath, dateValue);
                case "<=":
                    return criteriaBuilder.lessThanOrEqualTo(datePath, dateValue);
            }
        } else if (value instanceof Comparable) {
            Path<Comparable> comparablePath = (Path<Comparable>) path;
            Comparable comparableValue = (Comparable) value;

            switch (operator) {
                case ">":
                    return criteriaBuilder.greaterThan(comparablePath, comparableValue);
                case "<":
                    return criteriaBuilder.lessThan(comparablePath, comparableValue);
                case ">=":
                    return criteriaBuilder.greaterThanOrEqualTo(comparablePath, comparableValue);
                case "<=":
                    return criteriaBuilder.lessThanOrEqualTo(comparablePath, comparableValue);
            }
        }

        throw new IllegalArgumentException("Cannot apply comparison operator " + operator + " to value of type " + value.getClass());
    }

    /**
     * Builds a range predicate [min TO max].
     */
    @SuppressWarnings("unchecked")
    private static Predicate buildRangePredicate(
            Path<?> path,
            Object min,
            Object max,
            CriteriaBuilder criteriaBuilder) {

        List<Predicate> predicates = new ArrayList<>();

        if (min != null) {
            predicates.add(buildComparisonPredicate(path, min, criteriaBuilder, ">="));
        }

        if (max != null) {
            predicates.add(buildComparisonPredicate(path, max, criteriaBuilder, "<="));
        }

        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * Builds a predicate for enum substring matching.
     * Matches any enum constant that contains the search term (case-insensitive).
     * 
     * @param path the path to the enum field
     * @param searchValue the search term to match against enum constant names
     * @param criteriaBuilder the criteria builder
     * @return a predicate matching all enum values containing the search term, or disjunction if no matches
     */
    @SuppressWarnings("unchecked")
    private static Predicate buildEnumSubstringPredicate(Path<?> path, String searchValue, CriteriaBuilder criteriaBuilder) {
        String searchTerm = searchValue.toUpperCase();
        Class<? extends Enum> enumClass = (Class<? extends Enum>) path.getJavaType();
        
        // Build OR predicate for all enum values that contain the search term
        List<Predicate> enumPredicates = new ArrayList<>();
        for (Enum<?> enumValue : enumClass.getEnumConstants()) {
            if (enumValue.name().contains(searchTerm)) {
                enumPredicates.add(criteriaBuilder.equal(path, enumValue));
            }
        }
        
        if (enumPredicates.isEmpty()) {
            // No matching enum values, return always-false predicate
            return criteriaBuilder.disjunction();
        }
        
        return criteriaBuilder.or(enumPredicates.toArray(new Predicate[0]));
    }

    /**
     * Builds an exists/not exists predicate, handling both single-valued and collection-valued paths.
     * For collections, checks if the collection is not empty (EXISTS) or empty (NOT EXISTS).
     * For single values, checks if the value is not null (EXISTS) or null (NOT EXISTS).
     */
    @SuppressWarnings("unchecked")
    private static Predicate buildExistsPredicate(Path<?> path, CriteriaBuilder criteriaBuilder, boolean negated) {
        // Check if the path is a collection
        if (path.getModel() instanceof PluralAttribute) {
            // For collections, check if they are empty or not
            if (negated) {
                // NOT EXISTS: collection is empty
                return criteriaBuilder.isEmpty((Expression<Collection<?>>) path);
            } else {
                // EXISTS: collection is not empty
                return criteriaBuilder.isNotEmpty((Expression<Collection<?>>) path);
            }
        } else {
            // For single-valued attributes, check null/not null
            if (negated) {
                return criteriaBuilder.isNull(path);
            } else {
                return criteriaBuilder.isNotNull(path);
            }
        }
    }

    /**
     * Validates that the given path is not a collection field.
     * Throws QueryFilterRuntimeException if the path is a collection.
     *
     * @param path the path to validate
     * @param fieldPath the field path string for error reporting
     * @param operator the operator being applied (for error message)
     * @throws QueryFilterRuntimeException if the path is a collection
     */
    private static void validateNotCollection(Path<?> path, String fieldPath, String operator) {
        if (path.getModel() instanceof PluralAttribute) {
            List<String> fields = List.of(fieldPath + " (operator: " + operator + ", valid: .exists:true or .exists:false)");
            throw new QueryFilterRuntimeException(new InvalidParameterException(MessageKeys.ERROR_QUERY_INVALID_COLLECTION_OPERATOR, fields));
        }
    }
}
