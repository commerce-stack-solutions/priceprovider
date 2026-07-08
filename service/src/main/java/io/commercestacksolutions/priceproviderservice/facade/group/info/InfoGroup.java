package io.commercestacksolutions.priceproviderservice.facade.group.info;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfoGroup extends InfoAuditableRestEntity {

    /** Read-only map of path → id for parentRefs. Used by the UI to build navigation links. */
    private Map<String, String> parentRefIds;

    /** Read-only map of path → id for subRefs. Used by the UI to build navigation links. */
    private Map<String, String> subRefIds;

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
