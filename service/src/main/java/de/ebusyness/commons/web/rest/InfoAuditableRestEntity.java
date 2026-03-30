package de.ebusyness.commons.web.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

/**
 * Base class for info objects that include audit timestamp fields.
 * Provides createdAt and lastModifiedAt timestamps from auditable entities.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfoAuditableRestEntity {
    private OffsetDateTime createdAt;
    private OffsetDateTime lastModifiedAt;

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(OffsetDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}
