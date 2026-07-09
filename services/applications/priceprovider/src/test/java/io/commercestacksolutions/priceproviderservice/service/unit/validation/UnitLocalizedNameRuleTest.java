package io.commercestacksolutions.priceproviderservice.service.unit.validation;

import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.LanguageEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for UnitLocalizedNameRule to ensure localized name validation works correctly.
 */
public class UnitLocalizedNameRuleTest {

    private UnitLocalizedNameRule rule;
    private LanguageEntityRepository mockLanguageRepository;

    @BeforeEach
    public void setup() {
        mockLanguageRepository = Mockito.mock(LanguageEntityRepository.class);
        rule = new UnitLocalizedNameRule(mockLanguageRepository);
    }

    @Test
    public void testValidUnitWithAllMandatoryLanguages_ShouldBeValid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

        // Create unit with all mandatory language values
        UnitEntity unit = new UnitEntity("kg");
        Map<String, String> name = new HashMap<>();
        name.put("en", "Kilogram");
        name.put("de", "Kilogramm");
        unit.setName(name);

        List<Message> errors = rule.validate(unit);

        assertTrue(errors.isEmpty(), "Unit with all mandatory languages should be valid");
    }

    @Test
    public void testUnitMissingOneMandatoryLanguage_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

        // Create unit missing German
        UnitEntity unit = new UnitEntity("kg");
        Map<String, String> name = new HashMap<>();
        name.put("en", "Kilogram");
        unit.setName(name);

        List<Message> errors = rule.validate(unit);

        assertFalse(errors.isEmpty(), "Unit missing mandatory language should be invalid");
        assertEquals(1, errors.size());
        assertEquals(MessageKeys.ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE, errors.get(0).getMessageKey());
        assertTrue(errors.get(0).getFields().contains("name"));
    }

    @Test
    public void testUnitMissingMultipleMandatoryLanguages_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        LanguageEntity frLang = new LanguageEntity("fr");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang, frLang));

        // Create unit with only English
        UnitEntity unit = new UnitEntity("kg");
        Map<String, String> name = new HashMap<>();
        name.put("en", "Kilogram");
        unit.setName(name);

        List<Message> errors = rule.validate(unit);

        assertFalse(errors.isEmpty(), "Unit missing multiple mandatory languages should be invalid");
        assertEquals(1, errors.size());
        assertEquals(MessageKeys.ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE, errors.get(0).getMessageKey());
    }

    @Test
    public void testUnitWithEmptyMandatoryLanguageValue_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

        // Create unit with empty German value
        UnitEntity unit = new UnitEntity("kg");
        Map<String, String> name = new HashMap<>();
        name.put("en", "Kilogram");
        name.put("de", "   "); // Empty/whitespace value
        unit.setName(name);

        List<Message> errors = rule.validate(unit);

        assertFalse(errors.isEmpty(), "Unit with empty mandatory language value should be invalid");
        assertEquals(1, errors.size());
    }

    @Test
    public void testUnitWithNullName_ShouldBeValid() {
        // Setup mandatory languages
        LanguageEntity enLang = new LanguageEntity("en");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Collections.singletonList(enLang));

        // Create unit with null name
        UnitEntity unit = new UnitEntity("kg");
        unit.setName(null);

        List<Message> errors = rule.validate(unit);

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

        // Create unit with any name
        UnitEntity unit = new UnitEntity("kg");
        Map<String, String> name = new HashMap<>();
        name.put("en", "Kilogram");
        unit.setName(name);

        List<Message> errors = rule.validate(unit);

        assertTrue(errors.isEmpty(), "Unit should be valid when no mandatory languages exist");
    }
}
