package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tax rounding strategy using HALF_UP rounding mode (commercial rounding / kaufmännisches Runden).
 * 
 * This strategy:
 * - Rounds to 2 decimal places
 * - Uses HALF_UP rounding mode
 * - Calculates taxes per line item, not per invoice total
 * 
 * This is the default implementation and can be exchanged with other strategies if needed.
 */
@Component
public class HalfUpTaxRoundingStrategy implements TaxRoundingStrategy {
    
    private static final int SCALE = 2;
    private static final int DIVISION_SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    
    @Override
    public BigDecimal calculateTaxFromGross(BigDecimal grossPrice, BigDecimal taxRate) {
        if (grossPrice == null || taxRate == null) {
            return BigDecimal.ZERO;
        }
        
        // Convert percentage to decimal (e.g., 19% -> 0.19)
        BigDecimal taxRateDecimal = taxRate.movePointLeft(2);
        
        // Tax = Gross - (Gross / (1 + TaxRate))
        // Tax = Gross - Net
        BigDecimal divisor = BigDecimal.ONE.add(taxRateDecimal);
        BigDecimal netPrice = grossPrice.divide(divisor, DIVISION_SCALE, ROUNDING_MODE);
        BigDecimal tax = grossPrice.subtract(netPrice);
        
        return tax.setScale(SCALE, ROUNDING_MODE);
    }
    
    @Override
    public BigDecimal calculateTaxFromNet(BigDecimal netPrice, BigDecimal taxRate) {
        if (netPrice == null || taxRate == null) {
            return BigDecimal.ZERO;
        }
        
        // Convert percentage to decimal (e.g., 19% -> 0.19)
        BigDecimal taxRateDecimal = taxRate.movePointLeft(2);
        
        // Tax = Net * TaxRate
        BigDecimal tax = netPrice.multiply(taxRateDecimal);
        
        return tax.setScale(SCALE, ROUNDING_MODE);
    }
    
    @Override
    public BigDecimal calculateNetFromGross(BigDecimal grossPrice, BigDecimal taxRate) {
        if (grossPrice == null || taxRate == null) {
            return grossPrice;
        }
        
        // Convert percentage to decimal (e.g., 19% -> 0.19)
        BigDecimal taxRateDecimal = taxRate.movePointLeft(2);
        
        // Net = Gross / (1 + TaxRate)
        BigDecimal divisor = BigDecimal.ONE.add(taxRateDecimal);
        BigDecimal netPrice = grossPrice.divide(divisor, DIVISION_SCALE, ROUNDING_MODE);
        
        return netPrice.setScale(SCALE, ROUNDING_MODE);
    }
    
    @Override
    public BigDecimal calculateGrossFromNet(BigDecimal netPrice, BigDecimal taxRate) {
        if (netPrice == null || taxRate == null) {
            return netPrice;
        }
        
        // Convert percentage to decimal (e.g., 19% -> 0.19)
        BigDecimal taxRateDecimal = taxRate.movePointLeft(2);
        
        // Gross = Net * (1 + TaxRate)
        BigDecimal multiplier = BigDecimal.ONE.add(taxRateDecimal);
        BigDecimal grossPrice = netPrice.multiply(multiplier);
        
        return grossPrice.setScale(SCALE, ROUNDING_MODE);
    }
}
