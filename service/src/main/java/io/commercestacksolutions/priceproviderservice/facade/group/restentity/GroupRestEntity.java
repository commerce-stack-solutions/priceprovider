package io.commercestacksolutions.priceproviderservice.facade.group.restentity;

import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;
import io.commercestacksolutions.commons.web.rest.RestEntity;

import java.util.Set;
import java.util.UUID;

public class GroupRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesGroup> {
    private UUID id;
    private String path;
    private String name;
    private Set<String> parentRefs;
    private Set<String> subRefs;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getParentRefs() {
        return parentRefs;
    }

    public void setParentRefs(Set<String> parentRefs) {
        this.parentRefs = parentRefs;
    }

    public Set<String> getSubRefs() {
        return subRefs;
    }

    public void setSubRefs(Set<String> subRefs) {
        this.subRefs = subRefs;
    }
}
