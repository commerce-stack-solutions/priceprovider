package de.ebusyness.priceproviderservice.facade.taxclass;

import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.priceproviderservice.facade.taxclass.restentity.TaxClassListRestEntity;
import de.ebusyness.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for createOrUpdateAllTaxClasses functionality.
 * Tests error handling and partial success scenarios.
 */
@SpringBootTest
public class TaxClassCreateOrUpdateAllIntegrationTest {

    @Autowired
    private TaxClassFacade taxClassFacade;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    @BeforeEach
    public void setup() {
        // Clean up test tax classes
        taxClassRepository.findAll().stream()
            .filter(tc -> tc.getTaxClassId().startsWith("TEST-"))
            .forEach(tc -> {
                try {
                    taxClassRepository.delete(tc);
                } catch (Exception e) {
                    // Ignore
                }
            });
    }

    @Test
    public void testCreateOrUpdateAll_AllValid_ShouldSucceed() {
        List<TaxClassRestEntity> taxClasses = new ArrayList<>();
        
        // Create first tax class — countryRef is mandatory (@MetaMandatoryField on TaxClassEntity.countryRef)
        TaxClassRestEntity vat19 = new TaxClassRestEntity();
        vat19.setTaxClassId("TEST-VAT-19");
        vat19.setTaxRate(new BigDecimal("0.19"));
        vat19.setCountryRef("DE");
        taxClasses.add(vat19);
        
        // Create second tax class
        TaxClassRestEntity vat7 = new TaxClassRestEntity();
        vat7.setTaxClassId("TEST-VAT-7");
        vat7.setTaxRate(new BigDecimal("0.07"));
        vat7.setCountryRef("DE");
        taxClasses.add(vat7);

        // Execute
        TaxClassListRestEntity result = taxClassFacade.createOrUpdateAllTaxClasses(taxClasses);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(2, result.getItems().size());
        
        // Verify no errors
        for (TaxClassRestEntity entity : result.getItems()) {
            assertTrue(entity.getMessages() == null || entity.getMessages().isEmpty(), 
                "Expected no error messages but found: " + entity.getMessages());
        }
        
        // Verify tax classes exist in DB
        assertTrue(taxClassRepository.existsById("TEST-VAT-19"));
        assertTrue(taxClassRepository.existsById("TEST-VAT-7"));
    }

    @Test
    public void testCreateOrUpdateAll_WithMissingId_ShouldReturnPartialSuccess() {
        List<TaxClassRestEntity> taxClasses = new ArrayList<>();
        
        // Valid tax class with mandatory countryRef
        TaxClassRestEntity vat19 = new TaxClassRestEntity();
        vat19.setTaxClassId("TEST-VAT-19B");
        vat19.setTaxRate(new BigDecimal("0.19"));
        vat19.setCountryRef("DE");
        taxClasses.add(vat19);
        
        // Invalid tax class - missing ID
        TaxClassRestEntity invalid = new TaxClassRestEntity();
        invalid.setTaxRate(new BigDecimal("0.07"));
        taxClasses.add(invalid);
        
        // Valid tax class with mandatory countryRef
        TaxClassRestEntity vat7 = new TaxClassRestEntity();
        vat7.setTaxClassId("TEST-VAT-7B");
        vat7.setTaxRate(new BigDecimal("0.07"));
        vat7.setCountryRef("DE");
        taxClasses.add(vat7);

        // Execute
        TaxClassListRestEntity result = taxClassFacade.createOrUpdateAllTaxClasses(taxClasses);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(3, result.getItems().size());
        
        // First and third should succeed
        assertTrue(taxClassRepository.existsById("TEST-VAT-19B"));
        assertTrue(taxClassRepository.existsById("TEST-VAT-7B"));
        
        // Second should have error - convert to List to access by index
        List<TaxClassRestEntity> resultList = new ArrayList<>(result.getItems());
        TaxClassRestEntity errorEntity = resultList.get(1);
        assertNotNull(errorEntity.getMessages());
        assertFalse(errorEntity.getMessages().isEmpty());
        assertEquals(Message.MessageType.ERROR, errorEntity.getMessages().get(0).getType());
    }

    @Test
    public void testCreateOrUpdateAll_Update_ShouldSucceed() {
        // Create initial tax class (bypassing validator — direct repository save for setup)
        TaxClassEntity existing = new TaxClassEntity();
        existing.setTaxClassId("TEST-VAT-UPD");
        existing.setTaxRate(new BigDecimal("0.19"));
        existing.setCountryRef("DE");
        taxClassRepository.save(existing);

        // Update it via facade — countryRef is mandatory
        List<TaxClassRestEntity> taxClasses = new ArrayList<>();
        TaxClassRestEntity update = new TaxClassRestEntity();
        update.setTaxClassId("TEST-VAT-UPD");
        update.setTaxRate(new BigDecimal("0.21")); // Changed rate
        update.setCountryRef("DE");
        taxClasses.add(update);

        // Execute
        TaxClassListRestEntity result = taxClassFacade.createOrUpdateAllTaxClasses(taxClasses);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
        
        // Verify updated
        TaxClassEntity updated = taxClassRepository.findById("TEST-VAT-UPD").orElse(null);
        assertNotNull(updated);
        assertEquals(0, new BigDecimal("0.21").compareTo(updated.getTaxRate()));
    }

    @Test
    public void testCreateOrUpdateAll_EmptyList_ShouldReturnError() {
        List<TaxClassRestEntity> taxClasses = new ArrayList<>();

        // Execute
        TaxClassListRestEntity result = taxClassFacade.createOrUpdateAllTaxClasses(taxClasses);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getMessages());
        assertFalse(result.getMessages().isEmpty());
        assertEquals(Message.MessageType.ERROR, result.getMessages().get(0).getType());
    }

    @Test
    public void testCreateOrUpdateAll_TooManyItems_ShouldReturnError() {
        List<TaxClassRestEntity> taxClasses = new ArrayList<>();
        
        // Create more than 100 items
        for (int i = 0; i < 101; i++) {
            TaxClassRestEntity taxClass = new TaxClassRestEntity();
            taxClass.setTaxClassId("TEST-TC" + i);
            taxClass.setTaxRate(new BigDecimal("0.19"));
            taxClasses.add(taxClass);
        }

        // Execute
        TaxClassListRestEntity result = taxClassFacade.createOrUpdateAllTaxClasses(taxClasses);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getMessages());
        assertFalse(result.getMessages().isEmpty());
        assertEquals(Message.MessageType.ERROR, result.getMessages().get(0).getType());
    }

    @Test
    public void testCreateOrUpdateAll_MixedCreateAndUpdate_ShouldSucceed() {
        // Create existing tax class (direct repository save for setup)
        TaxClassEntity existing = new TaxClassEntity();
        existing.setTaxClassId("TEST-EXIST-TC");
        existing.setTaxRate(new BigDecimal("0.10"));
        existing.setCountryRef("DE");
        taxClassRepository.save(existing);

        List<TaxClassRestEntity> taxClasses = new ArrayList<>();
        
        // Update existing — countryRef is mandatory
        TaxClassRestEntity update = new TaxClassRestEntity();
        update.setTaxClassId("TEST-EXIST-TC");
        update.setTaxRate(new BigDecimal("0.12"));
        update.setCountryRef("DE");
        taxClasses.add(update);
        
        // Create new — countryRef is mandatory
        TaxClassRestEntity create = new TaxClassRestEntity();
        create.setTaxClassId("TEST-NEW-TC");
        create.setTaxRate(new BigDecimal("0.15"));
        create.setCountryRef("AT");
        taxClasses.add(create);

        // Execute
        TaxClassListRestEntity result = taxClassFacade.createOrUpdateAllTaxClasses(taxClasses);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        
        // Both should succeed
        assertTrue(taxClassRepository.existsById("TEST-EXIST-TC"));
        assertTrue(taxClassRepository.existsById("TEST-NEW-TC"));
        
        // Verify update
        TaxClassEntity updated = taxClassRepository.findById("TEST-EXIST-TC").orElse(null);
        assertEquals(0, new BigDecimal("0.12").compareTo(updated.getTaxRate()));
    }
}
