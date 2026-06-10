package io.commercestacksolutions.priceproviderservice.dataaccess;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.definitions.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BigDecimalPrecisionTest {

    @Autowired
    private PriceRowEntityRepository priceRowRepository;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    @Autowired
    private UnitEntityRepository unitRepository;

    @Test
    public void testPriceValuePrecision() {
        // Create a tax class with high precision
        TaxClassEntity taxClass = new TaxClassEntity();
        taxClass.setTaxClassId("test-tax");
        taxClass.setTaxRate(new BigDecimal("0.19"));
        taxClass = taxClassRepository.save(taxClass);

        // Create a price row with high precision
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId("test-product");
        priceRow.setPriceValue(new BigDecimal("19.999"));
        priceRow.setMinQuantity(new BigDecimal("0.001"));
        priceRow.setTaxClass(taxClass);
        priceRow.setPriceType(new PriceType("SALES_PRICE"));
        priceRow.setTaxIncluded(false);

        PriceRowEntity saved = priceRowRepository.save(priceRow);
        priceRowRepository.flush();

        // Retrieve and check precision
        PriceRowEntity retrieved = priceRowRepository.findById(saved.getId()).orElseThrow();
        
        System.out.println("Original priceValue: " + new BigDecimal("19.999"));
        System.out.println("Retrieved priceValue: " + retrieved.getPriceValue());
        System.out.println("Original minQuantity: " + new BigDecimal("0.001"));
        System.out.println("Retrieved minQuantity: " + retrieved.getMinQuantity());

        assertEquals(0, new BigDecimal("19.999").compareTo(retrieved.getPriceValue()), 
                "Price value should maintain precision");
        assertEquals(0, new BigDecimal("0.001").compareTo(retrieved.getMinQuantity()), 
                "Min quantity should maintain precision");
    }

    @Test
    public void testUnitFactorPrecision() {
        UnitEntity unit = new UnitEntity();
        unit.setSymbol("test-unit");
        unit.setMeasure("test");
        unit.setFactor(new BigDecimal("0.000001"));

        UnitEntity saved = unitRepository.save(unit);
        unitRepository.flush();

        UnitEntity retrieved = unitRepository.findById(saved.getSymbol()).orElseThrow();
        
        System.out.println("Original factor: " + new BigDecimal("0.000001"));
        System.out.println("Retrieved factor: " + retrieved.getFactor());

        assertEquals(0, new BigDecimal("0.000001").compareTo(retrieved.getFactor()), 
                "Unit factor should maintain precision");
    }

    @Test
    public void testTaxRatePrecision() {
        TaxClassEntity taxClass = new TaxClassEntity();
        taxClass.setTaxClassId("test-tax-precision");
        taxClass.setTaxRate(new BigDecimal("0.081"));

        TaxClassEntity saved = taxClassRepository.save(taxClass);
        taxClassRepository.flush();

        TaxClassEntity retrieved = taxClassRepository.findById(saved.getTaxClassId()).orElseThrow();
        
        System.out.println("Original taxRate: " + new BigDecimal("0.081"));
        System.out.println("Retrieved taxRate: " + retrieved.getTaxRate());

        assertEquals(0, new BigDecimal("0.081").compareTo(retrieved.getTaxRate()), 
                "Tax rate should maintain precision");
    }

    @Test
    public void testExtremePrecisionValues() {
        // Test very small factor
        UnitEntity smallUnit = new UnitEntity();
        smallUnit.setSymbol("nano-test");
        smallUnit.setMeasure("test");
        smallUnit.setFactor(new BigDecimal("0.000000001")); // 9 decimal places

        UnitEntity savedSmall = unitRepository.save(smallUnit);
        unitRepository.flush();

        UnitEntity retrievedSmall = unitRepository.findById(savedSmall.getSymbol()).orElseThrow();
        assertEquals(0, new BigDecimal("0.000000001").compareTo(retrievedSmall.getFactor()), 
                "Very small unit factor should maintain precision");

        // Test large factor
        UnitEntity largeUnit = new UnitEntity();
        largeUnit.setSymbol("mega-test");
        largeUnit.setMeasure("test");
        largeUnit.setFactor(new BigDecimal("1000000000.123456789")); // Large value with decimals

        UnitEntity savedLarge = unitRepository.save(largeUnit);
        unitRepository.flush();

        UnitEntity retrievedLarge = unitRepository.findById(savedLarge.getSymbol()).orElseThrow();
        assertEquals(0, new BigDecimal("1000000000.123456789").compareTo(retrievedLarge.getFactor()), 
                "Large unit factor should maintain precision");

        // Test price with 4 decimal places
        TaxClassEntity taxClass = new TaxClassEntity();
        taxClass.setTaxClassId("test-extreme");
        taxClass.setTaxRate(new BigDecimal("0.19"));
        taxClass = taxClassRepository.save(taxClass);

        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId("extreme-test");
        priceRow.setPriceValue(new BigDecimal("99999.9999")); // Max 4 decimals
        priceRow.setMinQuantity(new BigDecimal("0.0001")); // Min 4 decimals
        priceRow.setTaxClass(taxClass);
        priceRow.setPriceType(new PriceType("SALES_PRICE"));
        priceRow.setTaxIncluded(false);

        PriceRowEntity savedPrice = priceRowRepository.save(priceRow);
        priceRowRepository.flush();

        PriceRowEntity retrievedPrice = priceRowRepository.findById(savedPrice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("99999.9999").compareTo(retrievedPrice.getPriceValue()), 
                "Price with 4 decimals should maintain precision");
        assertEquals(0, new BigDecimal("0.0001").compareTo(retrievedPrice.getMinQuantity()), 
                "Min quantity with 4 decimals should maintain precision");
    }
}
