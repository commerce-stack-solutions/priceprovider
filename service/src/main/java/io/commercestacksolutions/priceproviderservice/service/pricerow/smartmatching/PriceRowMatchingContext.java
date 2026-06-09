package io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching;

import io.commercestacksolutions.priceproviderservice.domain.pricetype.PriceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Criteria object carrying all possible input fields for price row smart matching.
 *
 * <p>A {@link SmartMatchingStrategy} implementation receives this context and may use
 * whichever subset of fields is relevant to its matching logic.  New fields can be added
 * here without changing the strategy interface contract.
 */
public class PriceRowMatchingContext {

    private String pricedResourceId;
    private BigDecimal minQuantity;
    private String unitRef;
    private String currencyRef;
    private String taxClassRef;
    private boolean taxIncluded;
    private PriceType priceType;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private Set<String> groupRefs;

    public PriceRowMatchingContext() {
    }

    /**
     * Returns {@code true} if all mandatory matching fields are populated.
     * Strategies may call this guard before attempting a database lookup.
     */
    public boolean hasRequiredFields() {
        return pricedResourceId != null
                && minQuantity != null
                && unitRef != null
                && currencyRef != null
                && taxClassRef != null;
    }

    public String getPricedResourceId() {
        return pricedResourceId;
    }

    public void setPricedResourceId(String pricedResourceId) {
        this.pricedResourceId = pricedResourceId;
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

    public boolean isTaxIncluded() {
        return taxIncluded;
    }

    public void setTaxIncluded(boolean taxIncluded) {
        this.taxIncluded = taxIncluded;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
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

    @Override
    public String toString() {
        return "PriceRowMatchingContext{" +
                "pricedResourceId='" + pricedResourceId + '\'' +
                ", minQuantity=" + minQuantity +
                ", unitRef='" + unitRef + '\'' +
                ", currencyRef='" + currencyRef + '\'' +
                ", taxClassRef='" + taxClassRef + '\'' +
                ", taxIncluded=" + taxIncluded +
                ", priceType=" + priceType +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                ", groupRefs=" + groupRefs +
                '}';
    }
}
