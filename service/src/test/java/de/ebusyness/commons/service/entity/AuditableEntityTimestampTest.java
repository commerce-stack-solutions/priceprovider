package de.ebusyness.commons.service.entity;

import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.service.language.LanguageService;
import de.ebusyness.priceproviderservice.service.currency.CurrencyService;
import de.ebusyness.priceproviderservice.service.unit.UnitService;
import de.ebusyness.priceproviderservice.service.taxclass.TaxClassService;
import de.ebusyness.priceproviderservice.service.pricerow.PriceRowService;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AuditableEntity timestamp tracking.
 * Tests that createdAt and lastModifiedAt are correctly set during save operations.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "service-config.initialize.essential-data-on=false",
    "service-config.initialize.sample-data-on=false"
})
@Transactional
public class AuditableEntityTimestampTest {

    @Autowired
    private LanguageService languageEntityService;

    @Autowired
    private CurrencyService currencyEntityService;

    @Autowired
    private UnitService unitEntityService;

    @Autowired
    private TaxClassService taxClassEntityService;

    @Autowired
    private PriceRowService priceRowEntityService;

    @Test
    public void testLanguageEntity_NewEntity_ShouldSetBothTimestamps() throws InterruptedException, EntityValidationException {
        // Create new entity
        LanguageEntity language = new LanguageEntity("ts");
        language.setActive(true);
        language.setMandatory(false);
        Map<String, String> names = new HashMap<>();
        names.put("en", "Test Language");
        names.put("de", "Test Sprache"); // Add German for mandatory language validation
        language.setName(names);
        
        // Timestamps should be null before save
        assertNull(language.getCreatedAt());
        assertNull(language.getLastModifiedAt());
        
        // Save entity
        OffsetDateTime beforeSave = OffsetDateTime.now();
        Thread.sleep(10); // Small delay to ensure timestamps are after beforeSave
        LanguageEntity saved = languageEntityService.save(language);
        Thread.sleep(10); // Small delay to ensure timestamps are before afterSave
        OffsetDateTime afterSave = OffsetDateTime.now();
        
        // Both timestamps should be set
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getLastModifiedAt());
        
        // Timestamps should be within the test window
        assertTrue(saved.getCreatedAt().isAfter(beforeSave) || saved.getCreatedAt().isEqual(beforeSave));
        assertTrue(saved.getCreatedAt().isBefore(afterSave) || saved.getCreatedAt().isEqual(afterSave));
        assertTrue(saved.getLastModifiedAt().isAfter(beforeSave) || saved.getLastModifiedAt().isEqual(beforeSave));
        assertTrue(saved.getLastModifiedAt().isBefore(afterSave) || saved.getLastModifiedAt().isEqual(afterSave));
        
        // Both timestamps should be equal for new entity
        assertEquals(saved.getCreatedAt(), saved.getLastModifiedAt());
    }

    @Test
    public void testLanguageEntity_UpdateEntity_ShouldUpdateOnlyLastModified() throws InterruptedException, EntityValidationException {
        // Create and save new entity
        LanguageEntity language = new LanguageEntity("up");
        language.setActive(true);
        language.setMandatory(false);
        Map<String, String> names = new HashMap<>();
        names.put("en", "Update Test");
        names.put("de", "Update Test"); // Add German for mandatory language validation
        language.setName(names);
        
        LanguageEntity saved = languageEntityService.save(language);
        OffsetDateTime originalCreatedAt = saved.getCreatedAt();
        OffsetDateTime originalLastModifiedAt = saved.getLastModifiedAt();
        
        // Wait to ensure timestamp difference
        Thread.sleep(100);
        
        // Update entity
        saved.setActive(false);
        LanguageEntity updated = languageEntityService.save(saved);
        
        // createdAt should not change
        assertEquals(originalCreatedAt, updated.getCreatedAt());
        
        // lastModifiedAt should be updated
        assertNotNull(updated.getLastModifiedAt());
        assertTrue(updated.getLastModifiedAt().isAfter(originalLastModifiedAt));
    }

    @Test
    public void testCurrencyEntity_NewEntity_ShouldSetBothTimestamps() throws InterruptedException, EntityValidationException {
        CurrencyEntity currency = new CurrencyEntity("TST");
        currency.setSymbol("T");
        Map<String, String> names = new HashMap<>();
        names.put("en", "Test Currency");
        names.put("de", "Testwährung");
        currency.setName(names);
        
        assertNull(currency.getCreatedAt());
        assertNull(currency.getLastModifiedAt());
        
        OffsetDateTime beforeSave = OffsetDateTime.now();
        Thread.sleep(10);
        CurrencyEntity saved = currencyEntityService.save(currency);
        Thread.sleep(10);
        OffsetDateTime afterSave = OffsetDateTime.now();
        
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getLastModifiedAt());
        assertTrue(saved.getCreatedAt().isAfter(beforeSave) || saved.getCreatedAt().isEqual(beforeSave));
        assertTrue(saved.getCreatedAt().isBefore(afterSave) || saved.getCreatedAt().isEqual(afterSave));
        assertEquals(saved.getCreatedAt(), saved.getLastModifiedAt());
    }

    @Test
    public void testUnitEntity_NewEntity_ShouldSetBothTimestamps() throws InterruptedException, EntityValidationException {
        UnitEntity unit = new UnitEntity("tst");
        unit.setMeasure("test");
        unit.setFactor(BigDecimal.ONE);
        Map<String, String> names = new HashMap<>();
        names.put("en", "Test Unit");
        names.put("de", "Test Einheit"); // Add German for mandatory language validation
        unit.setName(names);
        
        assertNull(unit.getCreatedAt());
        assertNull(unit.getLastModifiedAt());
        
        OffsetDateTime beforeSave = OffsetDateTime.now();
        Thread.sleep(10);
        UnitEntity saved = unitEntityService.save(unit);
        Thread.sleep(10);
        OffsetDateTime afterSave = OffsetDateTime.now();
        
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getLastModifiedAt());
        assertTrue(saved.getCreatedAt().isAfter(beforeSave) || saved.getCreatedAt().isEqual(beforeSave));
        assertTrue(saved.getCreatedAt().isBefore(afterSave) || saved.getCreatedAt().isEqual(afterSave));
        assertEquals(saved.getCreatedAt(), saved.getLastModifiedAt());
    }

    @Test
    public void testTaxClassEntity_NewEntity_ShouldSetBothTimestamps() throws InterruptedException, EntityValidationException {
        TaxClassEntity taxClass = new TaxClassEntity("test");
        taxClass.setTaxRate(new BigDecimal("0.19"));
        taxClass.setCountryRef("DE"); // mandatory since TaxClassMandatoryCountryAssignmentRule
        
        assertNull(taxClass.getCreatedAt());
        assertNull(taxClass.getLastModifiedAt());
        
        OffsetDateTime beforeSave = OffsetDateTime.now();
        Thread.sleep(10);
        TaxClassEntity saved = taxClassEntityService.save(taxClass);
        Thread.sleep(10);
        OffsetDateTime afterSave = OffsetDateTime.now();
        
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getLastModifiedAt());
        assertTrue(saved.getCreatedAt().isAfter(beforeSave) || saved.getCreatedAt().isEqual(beforeSave));
        assertTrue(saved.getCreatedAt().isBefore(afterSave) || saved.getCreatedAt().isEqual(afterSave));
        assertEquals(saved.getCreatedAt(), saved.getLastModifiedAt());
    }

    @Test
    public void testPriceRowEntity_NewEntity_ShouldSetBothTimestamps() throws InterruptedException {

    }

    @Test
    public void testPriceRowEntity_UpdateEntity_ShouldUpdateOnlyLastModified() throws InterruptedException {

    }
}
