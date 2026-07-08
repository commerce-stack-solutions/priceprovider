package io.commercestacksolutions.priceproviderservice.facade.approle.info;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfoAppRole extends InfoAuditableRestEntity {

    /** Read-only map of name → id for permissionRefs. Used by the UI to build navigation links. */
    private Map<String, Long> permissionRefIds;

    public Map<String, Long> getPermissionRefIds() {
        return permissionRefIds;
    }

    public void setPermissionRefIds(Map<String, Long> permissionRefIds) {
        this.permissionRefIds = permissionRefIds;
    }
}
