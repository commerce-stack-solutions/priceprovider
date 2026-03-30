package de.ebusyness.priceproviderservice.service.language.validation;

import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.priceproviderservice.dataaccess.language.LanguageEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for LanguageLocalizedNameRule to ensure localized name validation works correctly.
 */
public class LanguageLocalizedNameRuleTest {

    private LanguageLocalizedNameRule rule;
    private LanguageEntityRepository mockLanguageRepository;

    @BeforeEach
    public void setup() {
        mockLanguageRepository = Mockito.mock(LanguageEntityRepository.class);
        rule = new LanguageLocalizedNameRule(mockLanguageRepository);
    }

    @Test
    public void testValidLanguageWithAllMandatoryLanguages_ShouldBeValid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

        // Create language with all mandatory language values
        LanguageEntity language = new LanguageEntity("fr");
        Map<String, String> name = new HashMap<>();
        name.put("en", "French");
        name.put("de", "Französisch");
        name.put("fr", "Français");
        language.setName(name);

        List<Message> errors = rule.validate(language);

        assertTrue(errors.isEmpty(), "Language with all mandatory languages should be valid");
    }

    @Test
    public void testLanguageMissingOneMandatoryLanguage_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

        // Create language missing German
        LanguageEntity language = new LanguageEntity("fr");
        Map<String, String> name = new HashMap<>();
        name.put("en", "French");
        name.put("fr", "Français");
        language.setName(name);

        List<Message> errors = rule.validate(language);

        assertFalse(errors.isEmpty(), "Language missing mandatory language should be invalid");
        assertEquals(1, errors.size());
        assertEquals(MessageKeys.ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE, errors.get(0).getMessageKey());
        assertTrue(errors.get(0).getFields().contains("name"));
    }

    @Test
    public void testLanguageMissingMultipleMandatoryLanguages_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        LanguageEntity frLang = new LanguageEntity("fr");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang, frLang));

        // Create language with only English
        LanguageEntity language = new LanguageEntity("es");
        Map<String, String> name = new HashMap<>();
        name.put("en", "Spanish");
        language.setName(name);

        List<Message> errors = rule.validate(language);

        assertFalse(errors.isEmpty(), "Language missing multiple mandatory languages should be invalid");
        assertEquals(1, errors.size());
        assertEquals(MessageKeys.ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE, errors.get(0).getMessageKey());
    }

    @Test
    public void testLanguageWithEmptyMandatoryLanguageValue_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

        // Create language with empty German value
        LanguageEntity language = new LanguageEntity("fr");
        Map<String, String> name = new HashMap<>();
        name.put("en", "French");
        name.put("de", "   "); // Empty/whitespace value
        language.setName(name);

        List<Message> errors = rule.validate(language);

        assertFalse(errors.isEmpty(), "Language with empty mandatory language value should be invalid");
        assertEquals(1, errors.size());
    }

    @Test
    public void testLanguageWithNullName_ShouldBeValid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Collections.singletonList(enLang));

        // Create language with null name
        LanguageEntity language = new LanguageEntity("fr");
        language.setName(null);

        List<Message> errors = rule.validate(language);

        assertTrue(errors.isEmpty(), "Validation should handle null name gracefully");
    }

    @Test
    public void testNullEntity_ShouldBeValid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Collections.singletonList(enLang));

        List<Message> errors = rule.validate(null);

        assertTrue(errors.isEmpty(), "Validation should handle null entity gracefully");
    }

    @Test
    public void testNoMandatoryLanguages_ShouldBeValid() {
        // No mandatory languages
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Collections.emptyList());

        // Create language with any name
        LanguageEntity language = new LanguageEntity("fr");
        Map<String, String> name = new HashMap<>();
        name.put("fr", "Français");
        language.setName(name);

        List<Message> errors = rule.validate(language);

        assertTrue(errors.isEmpty(), "Language should be valid when no mandatory languages exist");
    }
}
