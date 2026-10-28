package io.commercestacksolutions.corebusinessentities.service.language.validation;

import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.corebusinessentities.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.LanguageEntityRepository;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

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
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

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
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        LanguageEntity frLang = new LanguageEntity("fr");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang, frLang));

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
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

        LanguageEntity language = new LanguageEntity("fr");
        Map<String, String> name = new HashMap<>();
        name.put("en", "French");
        name.put("de", "   ");
        language.setName(name);

        List<Message> errors = rule.validate(language);

        assertFalse(errors.isEmpty(), "Language with empty mandatory language value should be invalid");
        assertEquals(1, errors.size());
    }

    @Test
    public void testLanguageWithNullName_ShouldBeValid() {
        LanguageEntity enLang = new LanguageEntity("en");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Collections.singletonList(enLang));

        LanguageEntity language = new LanguageEntity("fr");
        language.setName(null);

        List<Message> errors = rule.validate(language);

        assertTrue(errors.isEmpty(), "Validation should handle null name gracefully");
    }

    @Test
    public void testNullEntity_ShouldBeValid() {
        LanguageEntity enLang = new LanguageEntity("en");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Collections.singletonList(enLang));

        List<Message> errors = rule.validate(null);

        assertTrue(errors.isEmpty(), "Validation should handle null entity gracefully");
    }

    @Test
    public void testNoMandatoryLanguages_ShouldBeValid() {
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Collections.emptyList());

        LanguageEntity language = new LanguageEntity("fr");
        Map<String, String> name = new HashMap<>();
        name.put("fr", "Français");
        language.setName(name);

        List<Message> errors = rule.validate(language);

        assertTrue(errors.isEmpty(), "Language should be valid when no mandatory languages exist");
    }
}
