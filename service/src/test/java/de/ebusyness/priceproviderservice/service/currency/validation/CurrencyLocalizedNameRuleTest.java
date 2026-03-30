package de.ebusyness.priceproviderservice.service.currency.validation;

import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.language.LanguageEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

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
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

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
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        LanguageEntity frLang = new LanguageEntity("fr");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang, frLang));

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
        LanguageEntity enLang = new LanguageEntity("en");
        LanguageEntity deLang = new LanguageEntity("de");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Arrays.asList(enLang, deLang));

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
        LanguageEntity enLang = new LanguageEntity("en");
        when(mockLanguageRepository.findByMandatory(true)).thenReturn(Collections.singletonList(enLang));

        // Create currency with null name
        CurrencyEntity currency = new CurrencyEntity("USD");
        currency.setName(null);

        List<Message> errors = rule.validate(currency);

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

        // Create currency with any name
        CurrencyEntity currency = new CurrencyEntity("USD");
        Map<String, String> name = new HashMap<>();
        name.put("en", "US Dollar");
        currency.setName(name);

        List<Message> errors = rule.validate(currency);

        assertTrue(errors.isEmpty(), "Currency should be valid when no mandatory languages exist");
    }
}
