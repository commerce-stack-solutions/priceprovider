package io.commercestacksolutions.priceproviderservice.facade.unit.restentity;

import io.commercestacksolutions.commons.web.rest.RestEntity;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;

import java.math.BigDecimal;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.commercestacksolutions.priceproviderservice.facade.config.BigDecimalPlainSerializer;

public class UnitRestEntity extends RestEntity<InfoAuditableRestEntity,IncludesUnit> {
    private String symbol;
    private Map<String, String> name;
    private String measure;
    private String baseUnitRef;

    @JsonSerialize(using = BigDecimalPlainSerializer.class)
    private BigDecimal factor;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public String getBaseUnitRef() {
        return baseUnitRef;
    }

    public void setBaseUnitRef(String baseUnitRef) {
        this.baseUnitRef = baseUnitRef;
    }

    // formatting of small fractions 0.0000000000000001 is allowed
    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }
}