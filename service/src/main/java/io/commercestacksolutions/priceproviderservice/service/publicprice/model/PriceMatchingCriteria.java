package io.commercestacksolutions.priceproviderservice.service.publicprice.model;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.definitions.PriceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Criteria for matching and searching prices.
 * Contains all the parameters needed to find the best matching price.
 */
public class PriceMatchingCriteria {
    
    private String pricedResourceId;
    private BigDecimal quantity;
    private String unitRef;
    private String currencyRef;
    private PriceType priceType;
    private String groupId;  // Optional - null for no group context
    private String channelId;  // Optional - null for no channel filtering
    private String countryKey; // Optional - null for no country filtering
    private OffsetDateTime referenceDate;  // Default: now
    private TaxationMode taxationMode;  // Default: GROSS
    private Boolean taxIncludedFilter;  // null = include all, false = only net, true = only gross
    
    public enum TaxationMode {
        NET,           // Return net price (tax excluded)
        GROSS,         // Return gross price (tax included) - DEFAULT
        AS_DECLARED    // Return price as declared (taxIncluded flag determines if calculation needed)
    }
    
    public PriceMatchingCriteria() {
        this.referenceDate = OffsetDateTime.now();
        this.taxationMode = TaxationMode.GROSS;
    }
    
    public String getPricedResourceId() {
        return pricedResourceId;
    }
    
    public void setPricedResourceId(String pricedResourceId) {
        this.pricedResourceId = pricedResourceId;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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
    
    public PriceType getPriceType() {
        return priceType;
    }
    
    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getCountryKey() {
        return countryKey;
    }

    public void setCountryKey(String countryKey) {
        this.countryKey = countryKey;
    }

    public OffsetDateTime getReferenceDate() {
        return referenceDate;
    }
    
    public void setReferenceDate(OffsetDateTime referenceDate) {
        this.referenceDate = referenceDate;
    }
    
    public TaxationMode getTaxationMode() {
        return taxationMode;
    }
    
    public void setTaxationMode(TaxationMode taxationMode) {
        this.taxationMode = taxationMode;
    }

    public Boolean getTaxIncludedFilter() {
        return taxIncludedFilter;
    }

    public void setTaxIncludedFilter(Boolean taxIncludedFilter) {
        this.taxIncludedFilter = taxIncludedFilter;
    }

    @Override
    public String toString() {
        return "PriceMatchingCriteria{" +
                "pricedResourceId='" + pricedResourceId + '\'' +
                ", quantity=" + quantity +
                ", unitRef='" + unitRef + '\'' +
                ", currencyRef='" + currencyRef + '\'' +
                ", priceType=" + priceType +
                ", groupId='" + groupId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", countryKey='" + countryKey + '\'' +
                ", referenceDate=" + referenceDate +
                ", taxationMode=" + taxationMode +
                ", taxIncludedFilter=" + taxIncludedFilter +
                '}';
    }
}
