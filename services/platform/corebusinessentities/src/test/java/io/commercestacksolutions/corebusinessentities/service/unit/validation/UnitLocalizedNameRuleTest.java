package io.commercestacksolutions.corebusinessentities.service.unit.validation;

import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.corebusinessentities.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.LanguageEntityRepository;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.unit.entity.UnitEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

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
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        LanguageEntity deLang = mock(LanguageEntity.class); when(deLang.getIsoKey()).thenReturn("de");
        doReturn(Arrays.asList(enLang, deLang)).when(mockLanguageRepository).findByMandatory(true);

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
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        LanguageEntity deLang = mock(LanguageEntity.class); when(deLang.getIsoKey()).thenReturn("de");
        doReturn(Arrays.asList(enLang, deLang)).when(mockLanguageRepository).findByMandatory(true);

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
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        LanguageEntity deLang = mock(LanguageEntity.class); when(deLang.getIsoKey()).thenReturn("de");
        LanguageEntity frLang = mock(LanguageEntity.class); when(frLang.getIsoKey()).thenReturn("fr");
        doReturn(Arrays.asList(enLang, deLang, frLang)).when(mockLanguageRepository).findByMandatory(true);

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
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        LanguageEntity deLang = mock(LanguageEntity.class); when(deLang.getIsoKey()).thenReturn("de");
        doReturn(Arrays.asList(enLang, deLang)).when(mockLanguageRepository).findByMandatory(true);

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
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        doReturn(Collections.singletonList(enLang)).when(mockLanguageRepository).findByMandatory(true);

        // Create unit with null name
        UnitEntity unit = new UnitEntity("kg");
        unit.setName(null);

        List<Message> errors = rule.validate(unit);

        assertTrue(errors.isEmpty(), "Validation should handle null name gracefully");
    }

    @Test
    public void testNullEntity_ShouldBeValid() {
        // Setup mandatory languages
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        doReturn(Collections.singletonList(enLang)).when(mockLanguageRepository).findByMandatory(true);

        List<Message> errors = rule.validate(null);

        assertTrue(errors.isEmpty(), "Validation should handle null entity gracefully");
    }

    @Test
    public void testNoMandatoryLanguages_ShouldBeValid() {
        // No mandatory languages
        doReturn(Collections.emptyList()).when(mockLanguageRepository).findByMandatory(true);

        // Create unit with any name
        UnitEntity unit = new UnitEntity("kg");
        Map<String, String> name = new HashMap<>();
        name.put("en", "Kilogram");
        unit.setName(name);

        List<Message> errors = rule.validate(unit);

        assertTrue(errors.isEmpty(), "Unit should be valid when no mandatory languages exist");
    }
}
