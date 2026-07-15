package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HalfUpRoundingTaxCalculationStrategy to ensure correct tax calculations
 * using HALF_UP rounding mode.
 */
public class HalfUpRoundingTaxCalculationStrategyTest {
    
    private HalfUpRoundingTaxCalculationStrategy strategy;
    
    @BeforeEach
    public void setup() {
        strategy = new HalfUpRoundingTaxCalculationStrategy();
    }
    
    @Test
    public void testCalculateTaxFromGross_StandardRate() {
        // 119.00 gross with 19% tax should result in 19.00 tax
        BigDecimal grossPrice = new BigDecimal("119.00");
        BigDecimal taxRate = new BigDecimal("19.00"); // Stored as percentage
        
        BigDecimal taxValue = strategy.calculateTaxFromGross(grossPrice, taxRate);
        
        assertEquals(new BigDecimal("19.00"), taxValue);
    }
    
    @Test
    public void testCalculateTaxFromGross_ReducedRate() {
        // 107.00 gross with 7% tax should result in 7.00 tax
        BigDecimal grossPrice = new BigDecimal("107.00");
        BigDecimal taxRate = new BigDecimal("7.00"); // Stored as percentage
        
        BigDecimal taxValue = strategy.calculateTaxFromGross(grossPrice, taxRate);
        
        assertEquals(new BigDecimal("7.00"), taxValue);
    }
    
    @Test
    public void testCalculateTaxFromGross_WithRounding() {
        // 100.00 gross with 19% tax should result in 15.97 tax (rounded)
        BigDecimal grossPrice = new BigDecimal("100.00");
        BigDecimal taxRate = new BigDecimal("19.00"); // Stored as percentage
        
        BigDecimal taxValue = strategy.calculateTaxFromGross(grossPrice, taxRate);
        
        assertEquals(new BigDecimal("15.97"), taxValue);
    }

    @Test
    public void testCalculateTaxFromGross_PrecisionEdgeCase() {
        // 0.03 gross with 20% tax should result in 0.01 tax
        BigDecimal grossPrice = new BigDecimal("0.03");
        BigDecimal taxRate = new BigDecimal("20.00");

        BigDecimal taxValue = strategy.calculateTaxFromGross(grossPrice, taxRate);

        assertEquals(new BigDecimal("0.01"), taxValue);
    }
    
    @Test
    public void testCalculateTaxFromNet_StandardRate() {
        // 100.00 net with 19% tax should result in 19.00 tax
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal taxRate = new BigDecimal("19.00"); // Stored as percentage
        
        BigDecimal taxValue = strategy.calculateTaxFromNet(netPrice, taxRate);
        
        assertEquals(new BigDecimal("19.00"), taxValue);
    }
    
    @Test
    public void testCalculateTaxFromNet_ReducedRate() {
        // 100.00 net with 7% tax should result in 7.00 tax
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal taxRate = new BigDecimal("7.00"); // Stored as percentage
        
        BigDecimal taxValue = strategy.calculateTaxFromNet(netPrice, taxRate);
        
        assertEquals(new BigDecimal("7.00"), taxValue);
    }
    
    @Test
    public void testCalculateTaxFromNet_WithRounding() {
        // 99.99 net with 19% tax should result in 19.00 tax (rounded)
        BigDecimal netPrice = new BigDecimal("99.99");
        BigDecimal taxRate = new BigDecimal("19.00"); // Stored as percentage
        
        BigDecimal taxValue = strategy.calculateTaxFromNet(netPrice, taxRate);
        
        assertEquals(new BigDecimal("19.00"), taxValue);
    }
    
    @Test
    public void testCalculateNetFromGross() {
        // 119.00 gross with 19% tax should result in 100.00 net
        BigDecimal grossPrice = new BigDecimal("119.00");
        BigDecimal taxRate = new BigDecimal("19.00"); // Stored as percentage
        
        BigDecimal netPrice = strategy.calculateNetFromGross(grossPrice, taxRate);
        
        assertEquals(new BigDecimal("100.00"), netPrice);
    }
    
    @Test
    public void testCalculateGrossFromNet() {
        // 100.00 net with 19% tax should result in 119.00 gross
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal taxRate = new BigDecimal("19.00"); // Stored as percentage
        
        BigDecimal grossPrice = strategy.calculateGrossFromNet(netPrice, taxRate);
        
        assertEquals(new BigDecimal("119.00"), grossPrice);
    }
    
    @Test
    public void testCalculateTaxFromGross_NullInputs() {
        BigDecimal result = strategy.calculateTaxFromGross(null, new BigDecimal("19.00"));
        assertEquals(BigDecimal.ZERO, result);
        
        result = strategy.calculateTaxFromGross(new BigDecimal("100.00"), null);
        assertEquals(BigDecimal.ZERO, result);
        
        result = strategy.calculateTaxFromGross(null, null);
        assertEquals(BigDecimal.ZERO, result);
    }
    
    @Test
    public void testCalculateTaxFromNet_NullInputs() {
        BigDecimal result = strategy.calculateTaxFromNet(null, new BigDecimal("19.00"));
        assertEquals(BigDecimal.ZERO, result);
        
        result = strategy.calculateTaxFromNet(new BigDecimal("100.00"), null);
        assertEquals(BigDecimal.ZERO, result);
        
        result = strategy.calculateTaxFromNet(null, null);
        assertEquals(BigDecimal.ZERO, result);
    }
    
    @Test
    public void testCalculateNetFromGross_NullInputs() {
        BigDecimal grossPrice = new BigDecimal("119.00");
        BigDecimal result = strategy.calculateNetFromGross(null, new BigDecimal("19.00"));
        assertNull(result);
        
        result = strategy.calculateNetFromGross(grossPrice, null);
        assertEquals(grossPrice, result);
    }
    
    @Test
    public void testCalculateGrossFromNet_NullInputs() {
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal result = strategy.calculateGrossFromNet(null, new BigDecimal("19.00"));
        assertNull(result);
        
        result = strategy.calculateGrossFromNet(netPrice, null);
        assertEquals(netPrice, result);
    }
    
    @Test
    public void testRoundingMode_HalfUp() {
        // Test that HALF_UP rounding is used (0.5 rounds up)
        // 100.005 should round to 100.01
        BigDecimal netPrice = new BigDecimal("52.658");
        BigDecimal taxRate = new BigDecimal("19.00"); // Stored as percentage
        
        BigDecimal taxValue = strategy.calculateTaxFromNet(netPrice, taxRate);
        
        // 52.658 * 0.19 = 10.00502, which should round to 10.01
        assertEquals(new BigDecimal("10.01"), taxValue);
    }
}
