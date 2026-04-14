package io.commercestacksolutions.priceproviderservice.facade.approle.restentity;

import io.commercestacksolutions.commons.web.rest.RestEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.info.InfoAppRole;

import java.util.Set;

public class AppRoleRestEntity extends RestEntity<InfoAppRole, IncludesAppRole> {
    private Long id;
    private String name;
    private String description;
    private Set<String> permissionRefs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
