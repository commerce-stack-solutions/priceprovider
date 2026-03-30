package de.ebusyness.priceproviderservice.service.language;

import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import de.ebusyness.priceproviderservice.dataaccess.language.LanguageEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for LanguageService validation rules.
 * Tests the inactive + mandatory validation at the service layer.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "service-config.initialize.essential-data-on=false",
    "service-config.initialize.sample-data-on=false"
})
@Transactional
public class LanguageServiceValidationTest {

    @Autowired
    private LanguageService languageService;

    @Autowired
    private LanguageEntityRepository languageRepository;

    @BeforeEach
    public void setup() {
        languageRepository.deleteAll();
    }

    @Test
    public void testSaveLanguage_ActiveAndMandatory_ShouldSucceed() throws EntityValidationException {
        // active=true + mandatory=true is valid
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-en", "English");
        language.setActive(true);
        language.setMandatory(true);
        
        LanguageEntity saved = languageService.save(language);
        
        assertNotNull(saved);
        assertEquals("test-en", saved.getIsoKey());
        assertTrue(saved.getActive());
        assertTrue(saved.getMandatory());
    }

    @Test
    public void testSaveLanguage_ActiveAndNotMandatory_ShouldSucceed() throws EntityValidationException {
        // active=true + mandatory=false is valid
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-de", "German");
        language.setActive(true);
        language.setMandatory(false);
        
        LanguageEntity saved = languageService.save(language);
        
        assertNotNull(saved);
        assertEquals("test-de", saved.getIsoKey());
        assertTrue(saved.getActive());
        assertFalse(saved.getMandatory());
    }

    @Test
    public void testSaveLanguage_InactiveAndNotMandatory_ShouldSucceed() throws EntityValidationException {
        // active=false + mandatory=false is valid
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-fr", "French");
        language.setActive(false);
        language.setMandatory(false);
        
        LanguageEntity saved = languageService.save(language);
        
        assertNotNull(saved);
        assertEquals("test-fr", saved.getIsoKey());
        assertFalse(saved.getActive());
        assertFalse(saved.getMandatory());
    }

    @Test
    public void testSaveLanguage_InactiveAndMandatory_ShouldThrowException() {
        // active=false + mandatory=true is INVALID
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-es", "Spanish");
        language.setActive(false);
        language.setMandatory(true);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> languageService.save(language),
            "Should throw exception when language is inactive and mandatory"
        );
        
        assertFalse(exception.getMessages().isEmpty(), "Exception should have validation messages");
        String firstMessageKey = exception.getMessages().get(0).getMessageKey();
        assertEquals("common.errors.language.mandatoryMustBeActive", firstMessageKey, 
                "Exception message key should match expected value, but was: " + firstMessageKey);
    }

    @Test
    public void testUpdateLanguage_ChangeToInactiveAndMandatory_ShouldThrowException() throws EntityValidationException {
        // Create a valid language first
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-it", "Italian");
        language.setActive(true);
        language.setMandatory(false);
        
        LanguageEntity saved = languageService.save(language);
        assertNotNull(saved);
        
        // Now try to update to invalid state
        saved.setActive(false);
        saved.setMandatory(true);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> languageService.updateLanguage(saved),
            "Should throw exception when updating to inactive and mandatory"
        );
        
        assertFalse(exception.getMessages().isEmpty(), "Exception should have validation messages");
        String firstMessageKey = exception.getMessages().get(0).getMessageKey();
        assertEquals("common.errors.language.mandatoryMustBeActive", firstMessageKey, 
                "Exception message key should match expected value, but was: " + firstMessageKey);
    }

    @Test
    public void testUpdateLanguage_KeepValidState_ShouldSucceed() throws EntityValidationException {
        // Create and save a valid language
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-pt", "Portuguese");
        language.setActive(true);
        language.setMandatory(true);
        
        LanguageEntity saved = languageService.save(language);
        assertNotNull(saved);
        
        // Update to another valid state
        saved.setActive(true);
        saved.setMandatory(false);
        
        LanguageEntity updated = languageService.updateLanguage(saved);
        
        assertNotNull(updated);
        assertEquals("test-pt", updated.getIsoKey());
        assertTrue(updated.getActive());
        assertFalse(updated.getMandatory());
    }

    @Test
    public void testUpdateLanguage_MakeInactiveMandatoryLanguage_ShouldThrowException() throws EntityValidationException {
        // Create an active mandatory language
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-nl", "Dutch");
        language.setActive(true);
        language.setMandatory(true);
        
        LanguageEntity saved = languageService.save(language);
        assertNotNull(saved);
        
        // Try to make it inactive (while keeping mandatory=true)
        saved.setActive(false);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> languageService.updateLanguage(saved),
            "Should throw exception when making mandatory language inactive"
        );
        
        assertFalse(exception.getMessages().isEmpty(), "Exception should have validation messages");
        String firstMessageKey = exception.getMessages().get(0).getMessageKey();
        assertEquals("common.errors.language.mandatoryMustBeActive", firstMessageKey, 
                "Exception message key should match expected value, but was: " + firstMessageKey);
    }

    @Test
    public void testUpdateLanguage_MakeInactiveLanguageMandatory_ShouldThrowException() throws EntityValidationException {
        // Create an inactive non-mandatory language
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-sv", "Swedish");
        language.setActive(false);
        language.setMandatory(false);
        
        LanguageEntity saved = languageService.save(language);
        assertNotNull(saved);
        
        // Try to make it mandatory (while keeping active=false)
        saved.setMandatory(true);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> languageService.updateLanguage(saved),
            "Should throw exception when making inactive language mandatory"
        );
        
        assertFalse(exception.getMessages().isEmpty(), "Exception should have validation messages");
        String firstMessageKey = exception.getMessages().get(0).getMessageKey();
        assertEquals("common.errors.language.mandatoryMustBeActive", firstMessageKey, 
                "Exception message key should match expected value, but was: " + firstMessageKey);
    }

    @Test
    public void testSaveLanguage_WithNullActiveAndMandatory_ShouldThrowException() {
        // null active with mandatory=true should trigger validation error
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-pl", "Polish");

        language.setActive(null);
        language.setMandatory(true);

        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> languageService.save(language),
            "Should throw exception when language has null active and is mandatory"
        );

        assertFalse(exception.getMessages().isEmpty(), "Exception should have validation messages");
        String firstMessageKey = exception.getMessages().get(0).getMessageKey();
        assertEquals("common.errors.language.mandatoryMustBeActive", firstMessageKey,
                "Exception message key should match expected value, but was: " + firstMessageKey);
    }

    @Test
    public void testSaveLanguage_WithNullMandatory_ShouldSucceed() throws EntityValidationException {
        // active=false with null mandatory should not trigger validation error
        LanguageEntity language = createLanguageWithNameForAllMandatoryLanguages("test-da", "Danish");
        language.setActive(false);
        language.setMandatory(null);
        
        LanguageEntity saved = languageService.save(language);
        
        assertNotNull(saved);
        assertEquals("test-da", saved.getIsoKey());
    }

    private LanguageEntity createLanguageWithNameForAllMandatoryLanguages(String isoKey, String name) {
        LanguageEntity language = new LanguageEntity();
        language.setIsoKey(isoKey);
        Map<String, String> nameMap = new HashMap<>();
        List<LanguageEntity> mandatoryLanguages = languageService.getMandatoryLanguages();
        for (LanguageEntity mandatoryLanguage : mandatoryLanguages) {
            nameMap.put(mandatoryLanguage.getIsoKey(), name);
        }
        language.setName(nameMap);
        return language;
    }
}
