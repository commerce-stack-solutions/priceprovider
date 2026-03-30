package de.ebusyness.priceproviderservice.facade.organization.restentity;

import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.commons.web.rest.RestEntity;
import de.ebusyness.priceproviderservice.dataaccess.organization.enums.OrganizationType;

import java.util.Set;

public class OrganizationRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesOrganization> {
    private String id;
    private String name;
    private OrganizationType organizationType;
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

    public OrganizationType getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(OrganizationType organizationType) {
        this.organizationType = organizationType;
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
