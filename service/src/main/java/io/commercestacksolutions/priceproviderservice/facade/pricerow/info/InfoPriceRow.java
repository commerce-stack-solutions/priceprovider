package io.commercestacksolutions.priceproviderservice.facade.pricerow.info;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfoPriceRow extends InfoAuditableRestEntity {
    private TaxationInfo taxation;
    /** Read-only map of path → id for groupRefs. Used by the UI to build navigation links. */
    private Map<String, String> groupRefIds;

    public TaxationInfo getTaxation() {
        return taxation;
    }

    public void setTaxation(TaxationInfo taxation) {
        this.taxation = taxation;
    }

    public Map<String, String> getGroupRefIds() {
        return groupRefIds;
    }

    public void setGroupRefIds(Map<String, String> groupRefIds) {
        this.groupRefIds = groupRefIds;
    }
}
