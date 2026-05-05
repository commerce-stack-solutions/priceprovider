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
     *     validateEntity(entity);
     *     updateAuditTimestamps(entity);
     *
     *     // Fetch and detach existing entity for permission check
     *     MyEntity existingEntity = fetchAndDetachExistingEntity(
     *         entity.getId(), myEntityRepository, entityManager);
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
     * <p><b>Why detachment is necessary:</b></p>
     * <ul>
     *   <li>JPA manages entity state and automatically synchronizes changes across references to the same entity</li>
     *   <li>Without detachment, both "existingEntity" and the entity being saved would reference the same managed object</li>
     *   <li>Changes to the entity would be reflected in "existingEntity", making before/after checks ineffective</li>
     *   <li>Detachment creates a true snapshot of the database state before modification</li>
     * </ul>
     *
     * @param <T> the entity type
     * @param <ID> the ID type (typically String for this codebase, but can be Long or composite keys)
     * @param entityId the ID of the entity to fetch (null returns null)
     * @param repository the JPA repository for the entity type
     * @param entityManager the JPA EntityManager for detachment
     * @return the detached entity from the database, or null if the ID is null or entity not found
     */
    default <ID> T fetchAndDetachExistingEntity(ID entityId, JpaRepository<T, ID> repository, EntityManager entityManager) {
        if (entityId == null) {
            return null;
        }

        T existingEntity = repository.findById(entityId).orElse(null);
        if (existingEntity != null) {
            // Detach to ensure it won't be modified when we change the "after" entity
            entityManager.detach(existingEntity);
        }

        return existingEntity;
    }
}