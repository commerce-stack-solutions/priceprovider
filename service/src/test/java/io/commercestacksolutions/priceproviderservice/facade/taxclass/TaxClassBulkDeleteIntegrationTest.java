package io.commercestacksolutions.priceproviderservice.facade.taxclass;

import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.service.taxclass.TaxClassService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for TaxClass bulk delete operations.
 * Tests the behavior when deleting tax classes with and without foreign key references.
 */
@SpringBootTest
@ActiveProfiles("test")
public class TaxClassBulkDeleteIntegrationTest {

    @Autowired
    private TaxClassFacade taxClassFacade;

    @Autowired
    private TaxClassService taxClassService;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    @Autowired
    private PriceRowEntityRepository priceRowRepository;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    private TaxClassEntity testTaxClass1;
    private TaxClassEntity testTaxClass2;
    private TaxClassEntity testTaxClass3;
    private CurrencyEntity testCurrency;

    @BeforeEach
    public void setUp() {
        // Clean up any existing test data
        priceRowRepository.deleteAll();
        taxClassRepository.deleteAll();

        // Create test currency if it doesn't exist
        testCurrency = currencyRepository.findById("EUR").orElseGet(() -> {
            CurrencyEntity currency = new CurrencyEntity();
            currency.setCurrencyKey("EUR");
            currency.setCreatedAt(OffsetDateTime.now());
            currency.setLastModifiedAt(OffsetDateTime.now());
            return currencyRepository.save(currency);
        });

        // Create test tax classes
        testTaxClass1 = new TaxClassEntity();
        testTaxClass1.setTaxClassId("test-tax-1");
        testTaxClass1.setTaxRate(new BigDecimal("0.19"));
        testTaxClass1.setCreatedAt(OffsetDateTime.now());
        testTaxClass1.setLastModifiedAt(OffsetDateTime.now());
        testTaxClass1 = taxClassRepository.save(testTaxClass1);

        testTaxClass2 = new TaxClassEntity();
        testTaxClass2.setTaxClassId("test-tax-2");
        testTaxClass2.setTaxRate(new BigDecimal("0.07"));
        testTaxClass2.setCreatedAt(OffsetDateTime.now());
        testTaxClass2.setLastModifiedAt(OffsetDateTime.now());
        testTaxClass2 = taxClassRepository.save(testTaxClass2);

        testTaxClass3 = new TaxClassEntity();
        testTaxClass3.setTaxClassId("test-tax-3");
        testTaxClass3.setTaxRate(new BigDecimal("0.05"));
        testTaxClass3.setCreatedAt(OffsetDateTime.now());
        testTaxClass3.setLastModifiedAt(OffsetDateTime.now());
        testTaxClass3 = taxClassRepository.save(testTaxClass3);
    }

    @AfterEach
    public void tearDown() {
        // Clean up test data
        priceRowRepository.deleteAll();
        taxClassRepository.deleteAll();
    }

    @Test
    public void testBulkDeleteTaxClasses_Success() throws Exception {
        // Given: Three unreferenced tax classes exist
        assertTrue(taxClassRepository.findById("test-tax-1").isPresent());
        assertTrue(taxClassRepository.findById("test-tax-2").isPresent());
        assertTrue(taxClassRepository.findById("test-tax-3").isPresent());

        // When: Bulk delete is called
        taxClassFacade.bulkDeleteTaxClasses(List.of("test-tax-1", "test-tax-2", "test-tax-3"));

        // Then: All tax classes are deleted
        assertFalse(taxClassRepository.findById("test-tax-1").isPresent());
        assertFalse(taxClassRepository.findById("test-tax-2").isPresent());
        assertFalse(taxClassRepository.findById("test-tax-3").isPresent());
    }

    @Test
    public void testBulkDeleteTaxClasses_WithForeignKeyReference_ThrowsDataIntegrityException() {
        // Given: One tax class is referenced by a price row
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId("test-resource");
        priceRow.setPriceValue(new BigDecimal("100.00"));
        priceRow.setCurrency(testCurrency);
        priceRow.setMinQuantity(new BigDecimal("1"));
        priceRow.setTaxIncluded(false);
        priceRow.setTaxClass(testTaxClass1);  // Reference to test-tax-1
        priceRow.setCreatedAt(OffsetDateTime.now());
        priceRow.setLastModifiedAt(OffsetDateTime.now());
        priceRowRepository.save(priceRow);

        // When: Bulk delete is called including the referenced tax class
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            taxClassFacade.bulkDeleteTaxClasses(List.of("test-tax-1", "test-tax-2"));
        });

        // Then: DataIntegrityException is thrown
        assertNotNull(exception);
        assertNotNull(exception.getErrorResponse());
        assertFalse(exception.getMessages().isEmpty());

        // And: test-tax-2 should be deleted (it's not referenced) - PARTIAL DELETE
        assertFalse(taxClassRepository.findById("test-tax-2").isPresent());
        
        // And: test-tax-1 should still exist (it is referenced)
        assertTrue(taxClassRepository.findById("test-tax-1").isPresent());
    }

    @Test
    public void testBulkDeleteTaxClasses_NonExistentTaxClass_IgnoresAndContinues() throws Exception {
        // Given: Only two tax classes exist, one ID doesn't exist
        assertTrue(taxClassRepository.findById("test-tax-1").isPresent());
        assertTrue(taxClassRepository.findById("test-tax-2").isPresent());
        assertFalse(taxClassRepository.findById("non-existent-tax").isPresent());

        // When: Bulk delete is called with non-existent ID
        taxClassFacade.bulkDeleteTaxClasses(List.of("test-tax-1", "non-existent-tax", "test-tax-2"));

        // Then: Existing tax classes are deleted, non-existent is ignored
        assertFalse(taxClassRepository.findById("test-tax-1").isPresent());
        assertFalse(taxClassRepository.findById("test-tax-2").isPresent());
    }

    @Test
    public void testBulkDeleteTaxClasses_EmptyList_NoError() throws Exception {
        // Given: Tax classes exist
        assertTrue(taxClassRepository.findById("test-tax-1").isPresent());

        // When: Bulk delete is called with empty list
        taxClassFacade.bulkDeleteTaxClasses(List.of());

        // Then: No error occurs and tax classes still exist
        assertTrue(taxClassRepository.findById("test-tax-1").isPresent());
    }

    @Test
    public void testBulkDeleteTaxClasses_PartialList_WithReference_DeletesUnreferencedOnes() {
        // Given: One tax class is referenced, others are not
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId("test-resource");
        priceRow.setPriceValue(new BigDecimal("100.00"));
        priceRow.setCurrency(testCurrency);
        priceRow.setMinQuantity(new BigDecimal("1"));
        priceRow.setTaxIncluded(false);
        priceRow.setTaxClass(testTaxClass2);  // Reference to test-tax-2
        priceRow.setCreatedAt(OffsetDateTime.now());
        priceRow.setLastModifiedAt(OffsetDateTime.now());
        priceRowRepository.save(priceRow);

        // When: Bulk delete is called - test-tax-1 and test-tax-3 would delete, but test-tax-2 fails
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            taxClassFacade.bulkDeleteTaxClasses(List.of("test-tax-1", "test-tax-2", "test-tax-3"));
        });

        // Then: Exception is thrown
        assertNotNull(exception);

        // And: Unreferenced tax classes are deleted - PARTIAL DELETE
        assertFalse(taxClassRepository.findById("test-tax-1").isPresent());
        assertFalse(taxClassRepository.findById("test-tax-3").isPresent());
        
        // And: Referenced tax class still exists
        assertTrue(taxClassRepository.findById("test-tax-2").isPresent());
    }
}
