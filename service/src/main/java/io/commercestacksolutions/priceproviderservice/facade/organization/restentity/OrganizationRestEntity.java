package io.commercestacksolutions.priceproviderservice.facade.organization.restentity;

import io.commercestacksolutions.commons.web.rest.RestEntity;
import io.commercestacksolutions.priceproviderservice.facade.organization.info.InfoOrganization;

import java.util.Set;

public class OrganizationRestEntity extends RestEntity<InfoOrganization, IncludesOrganization> {
    private String id;
    private String path;
    private String name;
    private String organizationType;
    private Set<String> parentRefs;
    private Set<String> subRefs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(String organizationType) {
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
