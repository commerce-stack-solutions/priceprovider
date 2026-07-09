package io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity;

import io.commercestacksolutions.commons.web.rest.RestEntity;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;

import java.math.BigDecimal;

public class TaxClassRestEntity extends RestEntity<InfoAuditableRestEntity,IncludesTaxClass> {
    private String taxClassId;
    private BigDecimal taxRate;
    private String countryRef;

    public String getTaxClassId() {
        return taxClassId;
    }

    public void setTaxClassId(String taxClassId) {
        this.taxClassId = taxClassId;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getCountryRef() {
        return countryRef;
    }

    public void setCountryRef(String countryRef) {
        this.countryRef = countryRef;
    }
}
