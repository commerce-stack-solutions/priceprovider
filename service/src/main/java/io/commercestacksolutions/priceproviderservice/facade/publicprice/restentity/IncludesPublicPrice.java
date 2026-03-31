package io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitRestEntity;

/**
 * Includes section for public price response containing expanded related entities.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncludesPublicPrice {
    
    private UnitRestEntity unit;
    private CurrencyRestEntity currency;
    private TaxClassRestEntity taxClass;
    
    public UnitRestEntity getUnit() {
        return unit;
    }
    
    public void setUnit(UnitRestEntity unit) {
        this.unit = unit;
    }
    
    public CurrencyRestEntity getCurrency() {
        return currency;
    }
    
    public void setCurrency(CurrencyRestEntity currency) {
        this.currency = currency;
    }
    
    public TaxClassRestEntity getTaxClass() {
        return taxClass;
    }
    
    public void setTaxClass(TaxClassRestEntity taxClass) {
        this.taxClass = taxClass;
    }
}
