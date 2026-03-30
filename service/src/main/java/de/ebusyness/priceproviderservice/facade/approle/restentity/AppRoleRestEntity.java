package de.ebusyness.priceproviderservice.facade.approle.restentity;

import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.commons.web.rest.RestEntity;

import java.util.Set;

public class AppRoleRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesAppRole> {
    private String id;
    private String description;
    private Set<String> permissionRefs;

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

    public Set<String> getPermissionRefs() {
        return permissionRefs;
    }

    public void setPermissionRefs(Set<String> permissionRefs) {
        this.permissionRefs = permissionRefs;
    }
}
