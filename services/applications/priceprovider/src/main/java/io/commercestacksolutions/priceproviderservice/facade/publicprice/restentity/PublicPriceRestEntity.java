package io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity;

import io.commercestacksolutions.commons.web.rest.RestEntity;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.info.InfoPublicPrice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * REST entity for public price API responses.
 *
 * Contains the price information with calculated values based on the channel's
 * configured price representation mode.
 * The priceValue field contains the calculated price (net, gross, or as declared).
 * The taxIncluded field indicates whether the returned price includes tax.
 *
 * When $info is expanded, originalPrice section shows the original stored values before calculation.
 */
@Schema(description = "Public price information for a priced resource")
public class PublicPriceRestEntity extends RestEntity<InfoPublicPrice, IncludesPublicPrice> {
    
    @Schema(description = "Price row identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;
    
    @Schema(description = "Priced resource identifier (product, material, or service)", example = "PROD-001")
    private String pricedResourceId;
    
    @Schema(description = "Calculated price value (net, gross, or as declared based on channel price representation mode)", example = "99.99")
    private BigDecimal priceValue;
    
    @Schema(description = "Minimum quantity for this price to apply", example = "1.00")
    private BigDecimal minQuantity;
    
    @Schema(description = "Unit reference (symbol)", example = "pcs")
    private String unitRef;
    
    @Schema(description = "Currency reference (key)", example = "EUR")
    private String currencyRef;
    
    @Schema(description = "Tax class reference", example = "STANDARD")
    private String taxClassRef;

    @Schema(description = "Price type", example = "SALES_PRICE")
    private String priceType;

    @Schema(description = "Valid from date (ISO 8601)", example = "2024-01-01T00:00:00Z")
    private OffsetDateTime validFrom;
    
    @Schema(description = "Valid to date (ISO 8601)", example = "2024-12-31T23:59:59Z")
    private OffsetDateTime validTo;
    
    @Schema(description = "Group references (if price is group-specific)")
    private Set<String> groupRefs;

    @Schema(description = "Channel references")
    private Set<String> channelRefs;

    @Schema(description = "Indicates whether the returned price includes tax", example = "true")
    private boolean taxIncluded;
    
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
        return "PublicPriceRestEntity{" +
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
                ", taxIncluded=" + taxIncluded +
                '}';
    }
}
