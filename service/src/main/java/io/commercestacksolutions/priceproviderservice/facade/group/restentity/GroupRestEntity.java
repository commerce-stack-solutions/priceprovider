package io.commercestacksolutions.priceproviderservice.facade.group.restentity;

import io.commercestacksolutions.commons.web.rest.RestEntity;
import io.commercestacksolutions.priceproviderservice.facade.group.info.InfoGroup;

import java.util.Set;

public class GroupRestEntity extends RestEntity<InfoGroup, IncludesGroup> {
    private String id;
    private String path;
    private String name;
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
