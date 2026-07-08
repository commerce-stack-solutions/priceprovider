package io.commercestacksolutions.commons.dataaccess.entity;

import java.time.OffsetDateTime;

/**
 * Interface for entities that track creation and modification timestamps.
 * Entities implementing this interface will have their timestamps automatically
 * managed by the EntityService during save operations.
 */
public interface AuditableEntity {
    
    /**
     * Gets the timestamp when the entity was first created.
     * @return the creation timestamp
     */
    OffsetDateTime getCreatedAt();
    
    /**
     * Sets the timestamp when the entity was first created.
     * This should only be set once when the entity is first saved.
     * @param createdAt the creation timestamp
     */
    void setCreatedAt(OffsetDateTime createdAt);
    
    /**
     * Gets the timestamp when the entity was last modified.
     * @return the last modification timestamp
     */
    OffsetDateTime getLastModifiedAt();
    
    /**
     * Sets the timestamp when the entity was last modified.
     * This should be updated on every save operation.
     * @param lastModifiedAt the last modification timestamp
     */
    void setLastModifiedAt(OffsetDateTime lastModifiedAt);
}
