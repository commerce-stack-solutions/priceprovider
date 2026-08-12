package io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.commercestacksolutions.corebusinessentities.facade.currency.restentity.CurrencyRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.taxclass.restentity.TaxClassRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.unit.restentity.UnitRestEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncludesPriceRow {

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