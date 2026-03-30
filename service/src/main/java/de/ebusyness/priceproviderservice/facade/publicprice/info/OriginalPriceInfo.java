package de.ebusyness.priceproviderservice.facade.publicprice.info;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Original price information before tax calculation/conversion.
 * This is included in the $info section to show the original declared values.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OriginalPriceInfo {
    private BigDecimal originalPriceValue;
    private boolean originalTaxIncluded;
    
    public OriginalPriceInfo() {
    }
    
    public OriginalPriceInfo(BigDecimal originalPriceValue, boolean originalTaxIncluded) {
        this.originalPriceValue = originalPriceValue;
        this.originalTaxIncluded = originalTaxIncluded;
    }
    
    public BigDecimal getOriginalPriceValue() {
        return originalPriceValue;
    }
    
    public void setOriginalPriceValue(BigDecimal originalPriceValue) {
        this.originalPriceValue = originalPriceValue;
    }
    
    public boolean isOriginalTaxIncluded() {
        return originalTaxIncluded;
    }
    
    public void setOriginalTaxIncluded(boolean originalTaxIncluded) {
        this.originalTaxIncluded = originalTaxIncluded;
    }
}
