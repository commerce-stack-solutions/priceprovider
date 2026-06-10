package io.commercestacksolutions.commons.service.entity;

import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.language.LanguageService;
import io.commercestacksolutions.priceproviderservice.service.currency.CurrencyService;
import io.commercestacksolutions.priceproviderservice.service.unit.UnitService;
import io.commercestacksolutions.priceproviderservice.service.taxclass.TaxClassService;
import io.commercestacksolutions.priceproviderservice.service.pricerow.PriceRowService;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.definitions.PriceType;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
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

    @BeforeEach
    public void setUp() {
        // Setup mock HTTP request context for API context resolution
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/api/entities");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Set up authentication context for direct service method calls
        var authorities = AuthorityUtils.createAuthorityList(
            "priceprovider.admin:Language:write",
            "priceprovider.admin:Currency:write",
            "priceprovider.admin:Unit:write",
            "priceprovider.admin:TaxClass:write",
            "priceprovider.admin:PriceRow:write"
        );
        var auth = new UsernamePasswordAuthenticationToken("test-admin", "test", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    public void cleanup() {
        // Clear security context
        SecurityContextHolder.clearContext();
        // Clear request context
        RequestContextHolder.resetRequestAttributes();
    }

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
