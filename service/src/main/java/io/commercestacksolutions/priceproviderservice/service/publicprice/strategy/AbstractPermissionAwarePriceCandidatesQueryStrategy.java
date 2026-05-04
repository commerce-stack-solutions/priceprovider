package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.commons.permissionselector.PermissionFilterBuilder;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class for price candidate query strategies that enforce permission-based filtering.
 *
 * This class ensures that all custom query strategy implementations properly apply
 * user permission selectors at the database level. Subclasses must implement
 * {@link #buildBusinessLogicPredicates} to add their specific query logic, and this
 * base class will automatically append permission filtering predicates.
 *
 * <p><b>Why this abstraction?</b></p>
 * <ul>
 *   <li>Ensures all custom strategies apply permission filtering consistently</li>
 *   <li>Prevents security vulnerabilities from forgetting to add permission checks</li>
 *   <li>Centralizes permission filtering logic in one place</li>
 *   <li>Provides standardized trace logging for SQL queries</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * @Component
 * public class CustomPriceCandidatesQueryStrategy
 *         extends AbstractPermissionAwarePriceCandidatesQueryStrategy {
 *
 *     @Autowired
 *     public CustomPriceCandidatesQueryStrategy(
 *             EntityManager entityManager,
 *             PermissionFilterBuilder permissionFilterBuilder,
 *             AuthorizationContext authorizationContext) {
 *         super(entityManager, permissionFilterBuilder, authorizationContext);
 *     }
 *
 *     @Override
 *     protected void buildBusinessLogicPredicates(
 *             CriteriaBuilder cb,
 *             Root<PriceRowEntity> root,
 *             CriteriaQuery<?> query,
 *             List<Predicate> predicates,
 *             PriceCandidatesQueryParams params) {
 *
 *         // Add your custom query logic here
 *         predicates.add(cb.equal(root.get("pricedResourceId"), params.getPricedResourceId()));
 *         predicates.add(cb.equal(root.get("currencyRef").get("currencyKey"), params.getCurrencyRef()));
 *         // ... more business logic predicates
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractPermissionAwarePriceCandidatesQueryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPermissionAwarePriceCandidatesQueryStrategy.class);

    protected final EntityManager entityManager;
    protected final PermissionFilterBuilder permissionFilterBuilder;
    protected final AuthorizationContext authorizationContext;

    /**
     * Constructor for dependency injection.
     *
     * @param entityManager JPA EntityManager for query execution
     * @param permissionFilterBuilder Builder for converting permission selectors to JPA Specifications
     * @param authorizationContext Context providing current user's permissions
     */
    protected AbstractPermissionAwarePriceCandidatesQueryStrategy(
            EntityManager entityManager,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext) {
        this.entityManager = entityManager;
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
    }

    /**
     * Executes a JPA Criteria query with automatic permission filtering.
     *
     * This method orchestrates the query building process:
     * 1. Creates the JPA Criteria query infrastructure
     * 2. Calls {@link #buildBusinessLogicPredicates} for business logic
     * 3. Automatically adds permission-based filtering predicates
     * 4. Executes the query and logs it at TRACE level
     * 5. Returns the results
     *
     * @param params Query parameters object containing all filter criteria
     * @return List of price row entities matching the criteria and user permissions
     */
    protected final List<PriceRowEntity> executeQuery(PriceCandidatesQueryParams params) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);

        List<Predicate> predicates = new java.util.ArrayList<>();

        // Step 1: Build business logic predicates (implemented by subclass)
        buildBusinessLogicPredicates(cb, root, query, predicates, params);

        // Step 2: Add permission filtering predicates (handled by this base class)
        addPermissionFilteringPredicates(cb, root, query, predicates);

        // Step 3: Apply all predicates and set distinct
        query.where(cb.and(predicates.toArray(new Predicate[0])));
        query.distinct(true);

        // Step 4: Create typed query
        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);

        // Step 5: Log the SQL query at TRACE level if enabled
        logQueryIfTraceEnabled(typedQuery, params);

        // Step 6: Execute and return results
        return typedQuery.getResultList();
    }

    /**
     * Subclasses must implement this method to add their business logic predicates.
     *
     * This method is called before permission filtering is applied, allowing
     * the subclass to define its specific query logic (e.g., filtering by
     * pricedResourceId, currency, date range, groups, etc.).
     *
     * @param cb CriteriaBuilder for constructing predicates
     * @param root Root entity for the query (PriceRowEntity)
     * @param query CriteriaQuery being built
     * @param predicates List to which predicates should be added
     * @param params Query parameters containing all filter criteria
     */
    protected abstract void buildBusinessLogicPredicates(
        CriteriaBuilder cb,
        Root<PriceRowEntity> root,
        CriteriaQuery<?> query,
        List<Predicate> predicates,
        PriceCandidatesQueryParams params
    );

    /**
     * Adds permission-based filtering predicates to the query.
     *
     * This method is called automatically by {@link #executeQuery} and ensures
     * that user permission selectors are enforced at the database level.
     *
     * Permission filtering is skipped during bootstrap mode (data import).
     *
     * @param cb CriteriaBuilder for constructing predicates
     * @param root Root entity for the query (PriceRowEntity)
     * @param query CriteriaQuery being built
     * @param predicates List to which permission predicates will be added
     */
    private void addPermissionFilteringPredicates(
            CriteriaBuilder cb,
            Root<PriceRowEntity> root,
            CriteriaQuery<?> query,
            List<Predicate> predicates) {

        // Skip authorization checks during bootstrap/data import
        if (AuthorizationContext.isBootstrapMode()) {
            logger.debug("Bootstrap mode active - skipping permission filtering at query level");
            return;
        }

        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();

        try {
            Specification<PriceRowEntity> permissionSpec =
                permissionFilterBuilder.buildFilter(permissions, "PriceRow", "read");

            if (permissionSpec != null) {
                // Permission spec returned non-null means we need to apply filtering
                // Convert the Specification to a Predicate and add it to our predicates list
                Predicate permissionPredicate = permissionSpec.toPredicate(root, query, cb);
                if (permissionPredicate != null) {
                    predicates.add(permissionPredicate);
                    logger.debug("Applied permission-based filtering at database query level");
                }
            } else {
                // null spec means global permission without selector - no filtering needed
                logger.debug("Global permission found - no permission filtering applied at query level");
            }
        } catch (Exception e) {
            logger.error("Failed to build permission filter, denying all access: {}", e.getMessage());
            // On error, add a predicate that denies all access as a safety measure
            predicates.add(cb.disjunction()); // Always false
        }
    }

    /**
     * Logs the generated SQL query at TRACE level.
     *
     * This is useful for debugging and performance analysis. The SQL query
     * is extracted using Hibernate-specific APIs when available.
     *
     * @param typedQuery The JPA TypedQuery to be logged
     * @param params Query parameters for context
     */
    private void logQueryIfTraceEnabled(TypedQuery<PriceRowEntity> typedQuery, PriceCandidatesQueryParams params) {
        if (!logger.isTraceEnabled()) {
            return;
        }

        try {
            // Attempt to extract SQL using Hibernate-specific API
            org.hibernate.query.Query<?> hibernateQuery = typedQuery.unwrap(org.hibernate.query.Query.class);
            String sql = hibernateQuery.getQueryString();
            logger.trace("Executing price candidates query for pricedResourceId={}, SQL: {}",
                params.getPricedResourceId(), sql);
        } catch (Exception e) {
            // Fallback if Hibernate API is not available or fails
            logger.trace("Executing price candidates query for pricedResourceId={} (SQL extraction not available)",
                params.getPricedResourceId());
        }
    }
}
