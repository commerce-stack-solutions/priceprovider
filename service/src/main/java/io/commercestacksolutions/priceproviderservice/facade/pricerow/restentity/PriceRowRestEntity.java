package io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity;

import io.commercestacksolutions.commons.web.rest.RestEntity;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.info.InfoPriceRow;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitRestEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

public class PriceRowRestEntity extends RestEntity<InfoPriceRow, IncludesPriceRow> {
    private String id;
    private String pricedResourceId;  // a priced resource can be a product or material or something different
    private BigDecimal priceValue;
    private BigDecimal minQuantity;
    private String unitRef;
    private String currencyRef;
    private String taxClassRef;
    private String priceType;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private Set<String> groupRefs;
    private Set<String> channelRefs;
    private boolean taxIncluded;   // indicates if this is a net price or a price with tax included

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPricedResourceId() {
        return pricedResourceId;
    }

    public void setPricedResourceId(String pricedResourceId) {
        this.pricedResourceId = pricedResourceId;
    }

    public BigDecimal getPriceValue() {
        return priceValue;
    }

    public void setPriceValue(BigDecimal priceValue) {
        this.priceValue = priceValue;
    }

    public BigDecimal getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(BigDecimal minQuantity) {
        this.minQuantity = minQuantity;
    }

    public String getUnitRef() {
        return unitRef;
    }

    public void setUnitRef(String unitRef) {
        this.unitRef = unitRef;
    }

    public String getCurrencyRef() {
        return currencyRef;
    }

    public void setCurrencyRef(String currencyRef) {
        this.currencyRef = currencyRef;
    }

    public String getTaxClassRef() {
        return taxClassRef;
    }

    public void setTaxClassRef(String taxClassRef) {
        this.taxClassRef = taxClassRef;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
    }

    public Set<String> getGroupRefs() {
        return groupRefs;
    }

    public void setGroupRefs(Set<String> groupRefs) {
        this.groupRefs = groupRefs;
    }

    public Set<String> getChannelRefs() {
        return channelRefs;
    }

    public void setChannelRefs(Set<String> channelRefs) {
        this.channelRefs = channelRefs;
    }

    public boolean isTaxIncluded() {
        return taxIncluded;
    }

    public void setTaxIncluded(boolean taxIncluded) {
        this.taxIncluded = taxIncluded;
    }

    @Override
    public String toString() {
        return "PriceRowRestEntity{" +
                "id=" + id +
                ", pricedResourceId='" + pricedResourceId + '\'' +
                ", priceValue=" + priceValue +
                ", minQuantity=" + minQuantity +
                ", unitRef='" + unitRef + '\'' +
                ", currencyRef='" + currencyRef + '\'' +
                ", taxClassRef='" + taxClassRef + '\'' +
                ", priceType=" + priceType +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                ", groupRefs=" + groupRefs +
                ", channelRefs=" + channelRefs +
                ", taxIncluded=" + taxIncluded +
                '}';
    }
}