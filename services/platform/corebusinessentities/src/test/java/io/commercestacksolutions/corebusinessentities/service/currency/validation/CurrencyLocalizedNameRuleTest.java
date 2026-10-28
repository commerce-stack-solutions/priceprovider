package io.commercestacksolutions.corebusinessentities.service.currency.validation;

import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.corebusinessentities.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.corebusinessentities.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.LanguageEntityRepository;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for CurrencyLocalizedNameRule to ensure localized name validation works correctly.
 */
public class CurrencyLocalizedNameRuleTest {

    private CurrencyLocalizedNameRule rule;
    private LanguageEntityRepository mockLanguageRepository;

    @BeforeEach
    public void setup() {
        mockLanguageRepository = Mockito.mock(LanguageEntityRepository.class);
        rule = new CurrencyLocalizedNameRule(mockLanguageRepository);
    }

    @Test
    public void testValidCurrencyWithAllMandatoryLanguages_ShouldBeValid() {
        // Setup mandatory languages
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        LanguageEntity deLang = mock(LanguageEntity.class); when(deLang.getIsoKey()).thenReturn("de");
        doReturn(Arrays.asList(enLang, deLang)).when(mockLanguageRepository).findByMandatory(true);

        // Create currency with all mandatory language values
        CurrencyEntity currency = new CurrencyEntity("USD");
        Map<String, String> name = new HashMap<>();
        name.put("en", "US Dollar");
        name.put("de", "US-Dollar");
        currency.setName(name);

        List<Message> errors = rule.validate(currency);

        assertTrue(errors.isEmpty(), "Currency with all mandatory languages should be valid");
    }

    @Test
    public void testCurrencyMissingOneMandatoryLanguage_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        LanguageEntity deLang = mock(LanguageEntity.class); when(deLang.getIsoKey()).thenReturn("de");
        doReturn(Arrays.asList(enLang, deLang)).when(mockLanguageRepository).findByMandatory(true);

        // Create currency missing German
        CurrencyEntity currency = new CurrencyEntity("USD");
        Map<String, String> name = new HashMap<>();
        name.put("en", "US Dollar");
        currency.setName(name);

        List<Message> errors = rule.validate(currency);

        assertFalse(errors.isEmpty(), "Currency missing mandatory language should be invalid");
        assertEquals(1, errors.size());
        assertEquals(MessageKeys.ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE, errors.get(0).getMessageKey());
        assertTrue(errors.get(0).getFields().contains("name"));
    }

    @Test
    public void testCurrencyMissingMultipleMandatoryLanguages_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        LanguageEntity deLang = mock(LanguageEntity.class); when(deLang.getIsoKey()).thenReturn("de");
        LanguageEntity frLang = mock(LanguageEntity.class); when(frLang.getIsoKey()).thenReturn("fr");
        doReturn(Arrays.asList(enLang, deLang, frLang)).when(mockLanguageRepository).findByMandatory(true);

        // Create currency with only English
        CurrencyEntity currency = new CurrencyEntity("EUR");
        Map<String, String> name = new HashMap<>();
        name.put("en", "Euro");
        currency.setName(name);

        List<Message> errors = rule.validate(currency);

        assertFalse(errors.isEmpty(), "Currency missing multiple mandatory languages should be invalid");
        assertEquals(1, errors.size());
        assertEquals(MessageKeys.ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE, errors.get(0).getMessageKey());
    }

    @Test
    public void testCurrencyWithEmptyMandatoryLanguageValue_ShouldBeInvalid() {
        // Setup mandatory languages
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        LanguageEntity deLang = mock(LanguageEntity.class); when(deLang.getIsoKey()).thenReturn("de");
        doReturn(Arrays.asList(enLang, deLang)).when(mockLanguageRepository).findByMandatory(true);

        // Create currency with empty German value
        CurrencyEntity currency = new CurrencyEntity("USD");
        Map<String, String> name = new HashMap<>();
        name.put("en", "US Dollar");
        name.put("de", "   "); // Empty/whitespace value
        currency.setName(name);

        List<Message> errors = rule.validate(currency);

        assertFalse(errors.isEmpty(), "Currency with empty mandatory language value should be invalid");
        assertEquals(1, errors.size());
    }

    @Test
    public void testCurrencyWithNullName_ShouldBeValid() {
        // Setup mandatory languages
        LanguageEntity enLang = mock(LanguageEntity.class); when(enLang.getIsoKey()).thenReturn("en");
        doReturn(Collections.singletonList(enLang)).when(mockLanguageRepository).findByMandatory(true);

        // Create currency with null name
        CurrencyEntity currency = new CurrencyEntity("USD");
        currency.setName(null);

        List<Message> errors = rule.validate(currency);

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

        // Create currency with any name
        CurrencyEntity currency = new CurrencyEntity("USD");
        Map<String, String> name = new HashMap<>();
        name.put("en", "US Dollar");
        currency.setName(name);

        List<Message> errors = rule.validate(currency);

        assertTrue(errors.isEmpty(), "Currency should be valid when no mandatory languages exist");
    }
}
