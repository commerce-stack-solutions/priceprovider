package de.ebusyness.commons.service.entity;

import de.ebusyness.commons.dataaccess.entity.AuditableEntity;
import de.ebusyness.commons.service.entity.validation.EntityValidator;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.Message;

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
}