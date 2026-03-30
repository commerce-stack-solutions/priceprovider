package de.ebusyness.priceproviderservice.facade.approle.restentity;

import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.commons.web.rest.RestEntity;

public class AppPermissionRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesAppPermission> {
    private String id;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
