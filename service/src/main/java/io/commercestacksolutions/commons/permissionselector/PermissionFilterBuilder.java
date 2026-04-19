package io.commercestacksolutions.commons.permissionselector;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.PermissionNameParser.ParsedPermission;
import io.commercestacksolutions.commons.query.QueryExpression;
import io.commercestacksolutions.commons.query.QueryFilter;
import io.commercestacksolutions.commons.query.SpecificationBuilder;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Converts permission selectors into JPA Specifications for filtering list queries.
 *
 * <p>This component builds JPA Specifications from selector expressions, allowing
 * permission-based filtering of database queries. Multiple permissions are combined
 * with OR logic (union).
 *
 * <p><strong>Implementation Note:</strong> This implementation converts selector expressions
 * to QueryExpression format and uses the existing SpecificationBuilder. This leverages
 * the proven subquery-based approach for hasAny/hasAll operations on collections.
 *
 * <p>Usage:
 * <pre>
 * Specification&lt;PriceRowEntity&gt; spec = filterBuilder.buildFilter(permissions, "PriceRow", "read");
 * if (spec != null) {
 *     Page&lt;PriceRowEntity&gt; results = repository.findAll(spec, pageable);
 * } else {
 *     // Global permission, no filtering needed
 *     Page&lt;PriceRowEntity&gt; results = repository.findAll(pageable);
 * }
 * </pre>
 */
@Component
public class PermissionFilterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PermissionFilterBuilder.class);

    private final PermissionNameParser permissionNameParser = new PermissionNameParser();

    /**
     * Builds a JPA Specification from the user's permissions for a given dataType and action.
     *
     * @param permissions the user's permissions
     * @param dataType    the data type to filter (e.g., "PriceRow")
     * @param action      the action (e.g., "read")
     * @param <T>         the entity type
     * @return a Specification that filters entities according to the permissions, or null if no filtering needed
     * @throws InvalidParameterException if the selector expression cannot be converted to a valid query
     */
    public <T> Specification<T> buildFilter(Collection<AppPermissionEntity> permissions, String dataType, String action) throws InvalidParameterException {
        if (permissions == null || permissions.isEmpty()) {
            // No permissions = deny all
            logger.debug("No permissions for {}:{}, denying all access", dataType, action);
            return (root, query, cb) -> cb.disjunction(); // Always false
        }

        // Find all permissions matching dataType:action
        List<ParsedPermission> matchingPermissions = new ArrayList<>();
        for (AppPermissionEntity perm : permissions) {
            try {
                ParsedPermission parsed = permissionNameParser.parse(perm.getName());
                if (parsed.matchesTypeAndAction(dataType, action)) {
                    matchingPermissions.add(parsed);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse permission '{}': {}", perm.getName(), e.getMessage());
            }
        }

        if (matchingPermissions.isEmpty()) {
            // No matching permissions = deny all
            logger.debug("No matching permissions for {}:{}, denying all access", dataType, action);
            return (root, query, cb) -> cb.disjunction(); // Always false
        }

        // Check for global permission (no selector)
        boolean hasGlobalPermission = matchingPermissions.stream().anyMatch(p -> !p.hasSelector());
        if (hasGlobalPermission) {
            // Global permission = allow all, no filtering needed
            logger.debug("Global permission found for {}:{}, no filtering applied", dataType, action);
            return null;
        }

        // Build OR of all selector specifications
        List<QueryExpression> selectorExpressions = new ArrayList<>();
        for (ParsedPermission permission : matchingPermissions) {
            if (permission.hasSelector()) {
                try {
                    QueryExpression queryExpr = convertSelectorToQueryExpression(permission.getSelector());
                    selectorExpressions.add(queryExpr);
                } catch (Exception e) {
                    logger.warn("Failed to convert selector for permission '{}': {}",
                            permission.getFullName(), e.getMessage());
                }
            }
        }

        if (selectorExpressions.isEmpty()) {
            // No valid selector expressions = deny all
            logger.debug("No valid selector expressions for {}:{}, denying all access", dataType, action);
            return (root, query, cb) -> cb.disjunction(); // Always false
        }

        // Combine all expressions with OR
        QueryExpression combinedExpression;
        if (selectorExpressions.size() == 1) {
            combinedExpression = selectorExpressions.get(0);
        } else {
            combinedExpression = new QueryExpression(QueryExpression.LogicalOperator.OR, selectorExpressions);
        }

        // Use SpecificationBuilder to convert to JPA Specification
        return SpecificationBuilder.build(combinedExpression);
    }

    /**
     * Converts a SelectorExpression to a QueryExpression for use with SpecificationBuilder.
     * This allows us to leverage the existing, proven query building logic.
     *
     * @throws InvalidParameterException if the expression cannot be converted
     */
    private QueryExpression convertSelectorToQueryExpression(SelectorExpression selectorExpr) throws InvalidParameterException {
        if (selectorExpr.isLeaf()) {
            // Convert condition to QueryFilter
            SelectorCondition condition = selectorExpr.getCondition();
            QueryFilter.FilterOperator operator = mapSelectorOperatorToFilterOperator(condition.getOperator());

            Object value;
            if (condition.getOperator() == SelectorOperator.IS_EMPTY) {
                value = null; // NOT_EXISTS check
            } else if (condition.getOperator() == SelectorOperator.HAS_ANY ||
                       condition.getOperator() == SelectorOperator.HAS_ALL) {
                value = condition.getValues(); // List of values
            } else {
                value = condition.getValue(); // Single value
            }

            QueryFilter filter = new QueryFilter(condition.getField(), operator, value);
            return new QueryExpression(filter, selectorExpr.isNegated());
        } else {
            // Convert children recursively
            List<QueryExpression> children = new ArrayList<>();
            for (SelectorExpression child : selectorExpr.getChildren()) {
                children.add(convertSelectorToQueryExpression(child));
            }

            QueryExpression.LogicalOperator op = selectorExpr.getLogicalOperator() == SelectorExpression.LogicalOperator.AND
                    ? QueryExpression.LogicalOperator.AND
                    : QueryExpression.LogicalOperator.OR;

            return new QueryExpression(op, children, selectorExpr.isNegated());
        }
    }

    /**
     * Maps SelectorOperator to QueryFilter.FilterOperator.
     */
    private QueryFilter.FilterOperator mapSelectorOperatorToFilterOperator(SelectorOperator selectorOp) {
        switch (selectorOp) {
            case EQUALS:
                return QueryFilter.FilterOperator.EQUALS;
            case NOT_EQUALS:
                // NOT_EQUALS handled via negation in expression
                return QueryFilter.FilterOperator.EQUALS;
            case HAS_ANY:
                return QueryFilter.FilterOperator.HAS_ANY;
            case HAS_ALL:
                return QueryFilter.FilterOperator.HAS_ALL;
            case IS_EMPTY:
                return QueryFilter.FilterOperator.NOT_EXISTS;
            default:
                throw new SelectorEvaluationException("Unsupported operator for query conversion: " + selectorOp);
        }
    }
}
