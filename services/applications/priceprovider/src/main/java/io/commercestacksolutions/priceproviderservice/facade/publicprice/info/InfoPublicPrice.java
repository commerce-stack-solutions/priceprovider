package io.commercestacksolutions.priceproviderservice.facade.publicprice.info;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Info section for public price responses containing calculated values and original values.
 * This is included when $expand contains $info.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfoPublicPrice {
    private TaxationInfo taxation;
    private OriginalPriceInfo originalPrice;
    
    public TaxationInfo getTaxation() {
        return taxation;
    }
    
    public void setTaxation(TaxationInfo taxation) {
        this.taxation = taxation;
    }
    
    public OriginalPriceInfo getOriginalPrice() {
        return originalPrice;
    }
    
    public void setOriginalPrice(OriginalPriceInfo originalPrice) {
        this.originalPrice = originalPrice;
    }
}
