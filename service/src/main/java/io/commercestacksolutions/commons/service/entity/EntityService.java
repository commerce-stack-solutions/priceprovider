package io.commercestacksolutions.commons.service.entity;

import io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public interface EntityService<T> {
    T save(T entity) throws EntityValidationException;

    Class<T> getTargetClass();

    /**
     * Provides access to the entity validator for this service.
     * Implementations should return their configured EntityValidator instance.
     *
     * @return the entity validator, or null if no validation is configured
     */
    EntityValidator<T> getEntityValidator();

    /**
     * Returns the entity type name used for permission checks.
     * By default, returns the simple name of the target class with "Entity" suffix removed.
     *
     * @return the entity type name for authorization (e.g., "PriceRow", "Channel")
     */
    default String getEntityTypeName() {
        return getTargetClass().getSimpleName().replace("Entity", "");
    }

    /**
     * Validates the entity using the configured entity validator.
     * This default implementation applies all registered validation rules
     * and throws EntityValidationException if any validation errors occur.
     *
     * @param entity the entity to validate
     * @throws EntityValidationException if validation fails
     */
    default void validateEntity(T entity) throws EntityValidationException {
        EntityValidator<T> validator = getEntityValidator();
        if (validator != null) {
            List<Message> validationErrors = validator.validate(entity);
            if (!validationErrors.isEmpty()) {
                String entityName = getTargetClass().getSimpleName();
                throw new EntityValidationException(entityName + " validation failed", validationErrors);
            }
        }
    }

    /**
     * Updates timestamp fields for auditable entities.
     * Sets createdAt if null and always updates lastModifiedAt.
     * Timestamps are stored in UTC (no offset).
     *
     * @param entity the entity to update timestamps for
     */
    default void updateAuditTimestamps(T entity) {
        if (entity instanceof AuditableEntity) {
            AuditableEntity auditable = (AuditableEntity) entity;
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            if (auditable.getCreatedAt() == null) {
                auditable.setCreatedAt(now);
            }
            auditable.setLastModifiedAt(now);
        }
    }

    /**
     * Fetches the existing entity from the database and detaches it from the JPA persistence context.
     * This is crucial for before/after permission checks to ensure the "before" state is not
     * modified when the "after" state is changed.
     *
     * <p>This method is used in conjunction with
     * {@link io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService#checkAccessBeforeAndAfter}
     * to implement dual-state authorization checks on write and delete operations.</p>
     *
     * <p><b>Usage in service save() methods:</b></p>
     * <pre>
     * public MyEntity save(MyEntity entity) throws EntityValidationException {
     *     // Fetch and detach existing entity for permission check
     *     // Note: This will clear the persistence context, detaching the incoming entity
     *     MyEntity existingEntity = fetchAndDetachExistingEntity(
     *         entity.getId(), myEntityRepository, entityManager);
     *
     *     // Re-attach the incoming entity to the persistence context
     *     // This is necessary because fetchAndDetachExistingEntity clears the context
     *     if (entity.getId() != null) {
     *         entity = entityManager.merge(entity);
     *     }
     *
     *     validateEntity(entity);
     *     updateAuditTimestamps(entity);
     *
     *     // Check write permission on both before and after states
     *     entityAuthorizationService.checkAccessBeforeAndAfter(
     *         existingEntity, entity, getEntityTypeName(), "write",
     *         entity.getId() != null ? entity.getId() : "new");
     *
     *     return myEntityRepository.save(entity);
     * }
     * </pre>
     *
     * <p><b>Why this pattern is necessary:</b></p>
     * <ul>
     *   <li>JPA's first-level cache (persistence context) returns the same managed instance for repeated queries by ID</li>
     *   <li>If the incoming entity is already managed and modified, {@code entityManager.find()} would return that same instance</li>
     *   <li>We must clear the persistence context to force a fresh fetch from the database</li>
     *   <li>After clearing, the incoming entity becomes detached and must be re-attached with {@code merge()}</li>
     *   <li>The fetched entity is immediately detached to create an independent "before" snapshot</li>
     * </ul>
     *
     * <p><b>Important:</b> This method clears the entire persistence context using {@code entityManager.clear()}.
     * The calling code must re-attach the incoming entity using {@code entityManager.merge()} if it has an ID.</p>
     *
     * @param <T> the entity type
     * @param <ID> the ID type (typically String for this codebase, but can be Long or composite keys)
     * @param entityId the ID of the entity to fetch (null returns null)
     * @param repository the JPA repository for the entity type (not used, kept for API compatibility)
     * @param entityManager the JPA EntityManager for clearing context, fetching, and detachment
     * @return the detached entity from the database, or null if the ID is null or entity not found
     */
    default <ID> T fetchAndDetachExistingEntity(ID entityId, JpaRepository<T, ID> repository, EntityManager entityManager) {
        if (entityId == null) {
            return null;
        }

        // Clear the persistence context to remove all managed entities
        // This is necessary to force a fresh database fetch without getting the modified instance
        entityManager.clear();

        // Now fetch the entity from the database - it will be a fresh instance
        T existingEntity = entityManager.find(getTargetClass(), entityId);

        if (existingEntity != null) {
            // Detach immediately to create an independent snapshot
            entityManager.detach(existingEntity);
        }

        return existingEntity;
    }
}