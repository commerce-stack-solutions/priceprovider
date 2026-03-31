package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import java.math.BigDecimal;

/**
 * Strategy interface for tax rounding operations.
 * Implementations should follow tax calculation and rounding rules according to tax law requirements.
 * 
 * Following Open-Closed Principle - implementations can be exchanged without modifying existing code.
 */
public interface TaxRoundingStrategy {
    
    /**
     * Calculates the tax value from a price amount with tax included (gross price).
     * 
     * @param grossPrice the price with tax included
     * @param taxRate the tax rate (e.g., 0.19 for 19%)
     * @return the calculated tax value, properly rounded according to the strategy
     */
    BigDecimal calculateTaxFromGross(BigDecimal grossPrice, BigDecimal taxRate);
    
    /**
     * Calculates the tax value from a price amount with tax excluded (net price).
     * 
     * @param netPrice the price without tax
     * @param taxRate the tax rate (e.g., 0.19 for 19%)
     * @return the calculated tax value, properly rounded according to the strategy
     */
    BigDecimal calculateTaxFromNet(BigDecimal netPrice, BigDecimal taxRate);
    
    /**
     * Calculates the net price from a gross price.
     * 
     * @param grossPrice the price with tax included
     * @param taxRate the tax rate (e.g., 0.19 for 19%)
     * @return the calculated net price, properly rounded according to the strategy
     */
    BigDecimal calculateNetFromGross(BigDecimal grossPrice, BigDecimal taxRate);
    
    /**
     * Calculates the gross price from a net price.
     * 
     * @param netPrice the price without tax
     * @param taxRate the tax rate (e.g., 0.19 for 19%)
     * @return the calculated gross price, properly rounded according to the strategy
     */
    BigDecimal calculateGrossFromNet(BigDecimal netPrice, BigDecimal taxRate);
}
