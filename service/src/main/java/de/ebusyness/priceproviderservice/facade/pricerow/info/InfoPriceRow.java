package de.ebusyness.priceproviderservice.facade.pricerow.info;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfoPriceRow extends InfoAuditableRestEntity {
    private TaxationInfo taxation;

    public TaxationInfo getTaxation() {
        return taxation;
    }

    public void setTaxation(TaxationInfo taxation) {
        this.taxation = taxation;
    }
}
