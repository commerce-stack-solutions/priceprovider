package de.ebusyness.priceproviderservice.facade.currency;

import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for bulk delete currencies functionality.
 * Tests the proper handling of referential integrity constraints when deleting currencies
 * that are referenced by price rows.
 */
@SpringBootTest
public class CurrencyBulkDeleteIntegrationTest {

    @Autowired
    private CurrencyFacade currencyFacade;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @Autowired
    private PriceRowEntityRepository priceRowRepository;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    private TaxClassEntity testTaxClass;

    @BeforeEach
    public void setup() {
        // Clean up any existing test data from this test suite
        priceRowRepository.findAll().stream()
            .filter(pr -> pr.getPricedResourceId() != null && pr.getPricedResourceId().startsWith("product-"))
            .forEach(pr -> priceRowRepository.delete(pr));
        
        currencyRepository.findAll().stream()
            .filter(c -> c.getCurrencyKey().contains("-TEST"))
            .forEach(c -> {
                try {
                    currencyRepository.delete(c);
                } catch (Exception e) {
                    // Ignore - might be referenced
                }
            });
        
        // Make sure tax class exists
        testTaxClass = taxClassRepository.findById("TEST-TAX").orElse(null);
        if (testTaxClass == null) {
            testTaxClass = new TaxClassEntity();
            testTaxClass.setTaxClassId("TEST-TAX");
            testTaxClass.setTaxRate(new BigDecimal("0.19"));
            testTaxClass = taxClassRepository.save(testTaxClass);
        }
    }

    @Test
    public void testBulkDeleteCurrenciesNotInUse_ShouldSucceed() throws DataIntegrityException {
        // Create test currencies
        CurrencyEntity usd = createCurrency("USD-TEST", "US Dollar");
        CurrencyEntity eur = createCurrency("EUR-TEST", "Euro");
        CurrencyEntity gbp = createCurrency("GBP-TEST", "British Pound");

        // Verify currencies exist
        assertTrue(currencyRepository.existsById("USD-TEST"));
        assertTrue(currencyRepository.existsById("EUR-TEST"));
        assertTrue(currencyRepository.existsById("GBP-TEST"));

        // Bulk delete all test currencies
        List<String> currencyKeys = Arrays.asList("USD-TEST", "EUR-TEST", "GBP-TEST");
        currencyFacade.bulkDeleteCurrencies(currencyKeys);

        // Verify all currencies are deleted
        assertFalse(currencyRepository.existsById("USD-TEST"));
        assertFalse(currencyRepository.existsById("EUR-TEST"));
        assertFalse(currencyRepository.existsById("GBP-TEST"));
    }

    @Test
    public void testBulkDeleteCurrenciesInUse_ShouldThrowException() {
        // Create test currencies
        CurrencyEntity usd = createCurrency("USD-TEST2", "US Dollar");
        CurrencyEntity eur = createCurrency("EUR-TEST2", "Euro");
        CurrencyEntity gbp = createCurrency("GBP-TEST2", "British Pound");

        // Create price rows that reference USD and EUR
        createPriceRow("product-1", "USD-TEST2");
        createPriceRow("product-2", "EUR-TEST2");

        // Try to bulk delete all currencies (GBP is not in use, USD and EUR are in use)
        List<String> currencyKeys = Arrays.asList("USD-TEST2", "EUR-TEST2", "GBP-TEST2");
        
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            currencyFacade.bulkDeleteCurrencies(currencyKeys);
        });

        // Verify the exception contains information about the failed currencies
        assertNotNull(exception.getErrorResponse());
        assertFalse(exception.getMessages().isEmpty());

        // GBP should be deleted (it's not in use)
        assertFalse(currencyRepository.existsById("GBP-TEST2"));

        // USD and EUR should still exist (they are in use)
        assertTrue(currencyRepository.existsById("USD-TEST2"));
        assertTrue(currencyRepository.existsById("EUR-TEST2"));
    }

    @Test
    public void testBulkDeleteCurrenciesPartiallyInUse_ShouldDeleteUnreferencedOnes() {
        // Create test currencies
        CurrencyEntity usd = createCurrency("USD-TEST3", "US Dollar");
        CurrencyEntity eur = createCurrency("EUR-TEST3", "Euro");
        CurrencyEntity gbp = createCurrency("GBP-TEST3", "British Pound");
        CurrencyEntity jpy = createCurrency("JPY-TEST3", "Japanese Yen");

        // Create price row that references only USD
        createPriceRow("product-3", "USD-TEST3");

        // Try to bulk delete all currencies
        List<String> currencyKeys = Arrays.asList("USD-TEST3", "EUR-TEST3", "GBP-TEST3", "JPY-TEST3");
        
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            currencyFacade.bulkDeleteCurrencies(currencyKeys);
        });

        // Verify the exception is thrown
        assertNotNull(exception);

        // EUR, GBP, and JPY should be deleted (they are not in use)
        assertFalse(currencyRepository.existsById("EUR-TEST3"));
        assertFalse(currencyRepository.existsById("GBP-TEST3"));
        assertFalse(currencyRepository.existsById("JPY-TEST3"));

        // USD should still exist (it is in use)
        assertTrue(currencyRepository.existsById("USD-TEST3"));
    }

    @Test
    public void testBulkDeleteNonExistentCurrencies_ShouldNotThrowException() throws DataIntegrityException {
        // Try to delete currencies that don't exist
        List<String> currencyKeys = Arrays.asList("NON-EXISTENT-1", "NON-EXISTENT-2");
        
        // Should not throw exception, just silently skip non-existent currencies
        assertDoesNotThrow(() -> {
            currencyFacade.bulkDeleteCurrencies(currencyKeys);
        });
    }

    @Test
    public void testBulkDeleteEmptyList_ShouldNotThrowException() throws DataIntegrityException {
        // Try to delete with empty list
        List<String> currencyKeys = Arrays.asList();
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            currencyFacade.bulkDeleteCurrencies(currencyKeys);
        });
    }

    // Helper methods

    private CurrencyEntity createCurrency(String currencyKey, String name) {
        CurrencyEntity currency = new CurrencyEntity();
        currency.setCurrencyKey(currencyKey);
        currency.setSymbol(currencyKey.substring(0, 3));
        return currencyRepository.save(currency);
    }

    private PriceRowEntity createPriceRow(String resourceId, String currencyKey) {
        // Get the currency entity
        CurrencyEntity currency = currencyRepository.findById(currencyKey)
            .orElseThrow(() -> new RuntimeException("Currency not found: " + currencyKey));
        
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId(resourceId);
        priceRow.setPriceValue(new BigDecimal("100.00"));
        priceRow.setMinQuantity(new BigDecimal("1.0"));
        priceRow.setCurrency(currency);
        priceRow.setTaxClass(testTaxClass);
        priceRow.setPriceType(PriceType.SALES_PRICE);
        priceRow.setTaxIncluded(false);
        return priceRowRepository.save(priceRow);
    }
}
