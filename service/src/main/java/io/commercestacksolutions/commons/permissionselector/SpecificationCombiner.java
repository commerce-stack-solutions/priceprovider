package io.commercestacksolutions.commons.permissionselector;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.QueryExpression;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.SpecificationBuilder;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Utility component for combining permission-based specifications with user query specifications.
 *
 * <p>This component encapsulates the common pattern used across entity services for filtering
 * results based on both:
 * <ul>
 *   <li>Permission selectors (derived from user's AppPermissions)</li>
 *   <li>User-provided query filters (optional Lucene-like query strings)</li>
 * </ul>
 *
 * <p>The combination logic is:
 * <ul>
 *   <li>If both filters exist: AND them together</li>
 *   <li>If only permission filter exists: use it alone</li>
 *   <li>If only query filter exists: use it alone (user has global permission)</li>
 *   <li>If neither exists: no filtering needed (null specification)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * &#64;Autowired
 * private SpecificationCombiner specificationCombiner;
 *
 * public Page&lt;ChannelEntity&gt; getChannels(..., String query) throws QueryParseException, InvalidParameterException {
 *     Specification&lt;ChannelEntity&gt; spec = specificationCombiner.combine(
 *         permissions, "Channel", "read", query, queryParser);
 *
 *     if (spec != null) {
 *         return repository.findAll(spec, pageRequest);
 *     } else {
 *         return repository.findAll(pageRequest);
 *     }
 * }
 * </pre>
 */
@Component
public class SpecificationCombiner {

    private final PermissionFilterBuilder permissionFilterBuilder;

    public SpecificationCombiner(PermissionFilterBuilder permissionFilterBuilder) {
        this.permissionFilterBuilder = permissionFilterBuilder;
    }

    /**
     * Combines permission-based filtering with user query filtering.
     *
     * @param permissions the user's current permissions (from AuthorizationContext)
     * @param entityType the entity type name (e.g., "Channel", "PriceRow")
     * @param action the action being performed (e.g., "read", "write")
     * @param userQuery optional user-provided query string (may be null or empty)
     * @param queryParser the QueryParser configured with the entity's field types
     * @param <T> the entity type
     * @return combined specification, or null if no filtering is needed
     * @throws QueryParseException if the user query cannot be parsed
     * @throws InvalidParameterException if permission selector expressions are invalid
     */
    public <T> Specification<T> combine(
            Set<AppPermissionEntity> permissions,
            String entityType,
            String action,
            String userQuery,
            QueryParser queryParser) throws QueryParseException, InvalidParameterException {

        // Build specification from permission selectors
        Specification<T> permissionSpec = permissionFilterBuilder.buildFilter(permissions, entityType, action);

        // Build specification from user query (if provided)
        Specification<T> querySpec = null;
        if (userQuery != null && !userQuery.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(userQuery);
            querySpec = SpecificationBuilder.build(expression);
        }

        // Combine specifications
        if (permissionSpec != null && querySpec != null) {
            // Both permission filter and query filter present: AND them together
            return permissionSpec.and(querySpec);
        } else if (permissionSpec != null) {
            // Only permission filter
            return permissionSpec;
        } else if (querySpec != null) {
            // Only query filter (user has global permission)
            return querySpec;
        } else {
            // No filters at all (global permission, no query)
            return null;
        }
    }

    /**
     * Builds only the permission-based specification without any user query.
     *
     * <p>This is useful for simpler methods that don't support user query filtering.
     *
     * @param permissions the user's current permissions (from AuthorizationContext)
     * @param entityType the entity type name (e.g., "Channel", "PriceRow")
     * @param action the action being performed (e.g., "read", "write")
     * @param <T> the entity type
     * @return permission-based specification, or null if user has global permission
     * @throws InvalidParameterException if permission selector expressions are invalid
     */
    public <T> Specification<T> fromPermissions(
            Set<AppPermissionEntity> permissions,
            String entityType,
            String action) throws InvalidParameterException {
        return permissionFilterBuilder.buildFilter(permissions, entityType, action);
    }
}
