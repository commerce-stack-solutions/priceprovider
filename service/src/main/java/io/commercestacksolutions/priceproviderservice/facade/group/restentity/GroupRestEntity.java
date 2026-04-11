package io.commercestacksolutions.priceproviderservice.facade.group.restentity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;
import io.commercestacksolutions.commons.web.rest.RestEntity;

import java.util.Map;
import java.util.Set;

public class GroupRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesGroup> {
    private String id;
    private String path;
    private String name;
    private Set<String> parentRefs;
    private Set<String> subRefs;

    /** Read-only map of path → id for parentRefs. Used by the UI to build navigation links. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Map<String, String> parentRefIds;

    /** Read-only map of path → id for subRefs. Used by the UI to build navigation links. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Map<String, String> subRefIds;

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

    public Map<String, String> getParentRefIds() {
        return parentRefIds;
    }

    public void setParentRefIds(Map<String, String> parentRefIds) {
        this.parentRefIds = parentRefIds;
    }

    public Map<String, String> getSubRefIds() {
        return subRefIds;
    }

    public void setSubRefIds(Map<String, String> subRefIds) {
        this.subRefIds = subRefIds;
    }
}
