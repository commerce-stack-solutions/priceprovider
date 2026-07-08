package io.commercestacksolutions.priceproviderservice.facade.publicprice.info;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Taxation information for public price response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxationInfo {
    private BigDecimal taxValue;
    private BigDecimal taxRate;
    private String taxIncludedInfo;
    
    public TaxationInfo() {
    }
    
    public TaxationInfo(BigDecimal taxValue, BigDecimal taxRate, String taxIncludedInfo) {
        this.taxValue = taxValue;
        this.taxRate = taxRate;
        this.taxIncludedInfo = taxIncludedInfo;
    }
    
    public BigDecimal getTaxValue() {
        return taxValue;
    }
    
    public void setTaxValue(BigDecimal taxValue) {
        this.taxValue = taxValue;
    }
    
    public BigDecimal getTaxRate() {
        return taxRate;
    }
    
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
    
    public String getTaxIncludedInfo() {
        return taxIncludedInfo;
    }
    
    public void setTaxIncludedInfo(String taxIncludedInfo) {
        this.taxIncludedInfo = taxIncludedInfo;
    }
}
