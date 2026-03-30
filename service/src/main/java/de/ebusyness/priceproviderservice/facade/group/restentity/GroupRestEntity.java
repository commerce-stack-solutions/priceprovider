package de.ebusyness.priceproviderservice.facade.group.restentity;

import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.commons.web.rest.RestEntity;

import java.util.Set;

public class GroupRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesGroup> {
    private String id;
    private String name;
    private Set<String> parentRefs;
    private Set<String> subRefs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
