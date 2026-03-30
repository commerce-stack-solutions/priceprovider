package de.ebusyness.priceproviderservice.service.language.validation;

import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import de.ebusyness.commons.web.rest.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LanguageInactiveMandatoryRule to ensure that the validation rule
 * correctly detects invalid combinations of inactive + mandatory flags.
 * Tests the ValidationRule interface implementation.
 */
public class LanguageInactiveMandatoryRuleTest {

    private LanguageInactiveMandatoryRule validator;

    @BeforeEach
    public void setup() {
        validator = new LanguageInactiveMandatoryRule();
    }

    @Test
    public void testActiveAndMandatory_ShouldBeValid() {
        // active=true + mandatory=true is valid
        LanguageEntity language = new LanguageEntity("en");
        language.setActive(true);
        language.setMandatory(true);
        
        List<Message> errors = validator.validate(language);
        
        assertTrue(errors.isEmpty(), "Active and mandatory language should be valid");
    }

    @Test
    public void testActiveAndNotMandatory_ShouldBeValid() {
        // active=true + mandatory=false is valid
        LanguageEntity language = new LanguageEntity("de");
        language.setActive(true);
        language.setMandatory(false);
        
        List<Message> errors = validator.validate(language);
        
        assertTrue(errors.isEmpty(), "Active and non-mandatory language should be valid");
    }

    @Test
    public void testInactiveAndNotMandatory_ShouldBeValid() {
        // active=false + mandatory=false is valid
        LanguageEntity language = new LanguageEntity("fr");
        language.setActive(false);
        language.setMandatory(false);
        
        List<Message> errors = validator.validate(language);
        
        assertTrue(errors.isEmpty(), "Inactive and non-mandatory language should be valid");
    }

    @Test
    public void testInactiveAndMandatory_ShouldBeInvalid() {
        // active=false + mandatory=true is INVALID
        LanguageEntity language = new LanguageEntity("es");
        language.setActive(false);
        language.setMandatory(true);
        
        List<Message> errors = validator.validate(language);
        
        assertFalse(errors.isEmpty(), "Inactive and mandatory language should be invalid");
        assertEquals(1, errors.size(), "Should have exactly one validation error");
        
        Message error = errors.get(0);
        assertEquals(Message.MessageType.ERROR, error.getType(), "Should be an ERROR message");
        assertEquals("common.errors.language.mandatoryMustBeActive", error.getMessageKey(), 
                "Error message key should be correct");
        assertNotNull(error.getFields(), "Error should have fields specified");
        assertTrue(error.getFields().contains("active"), "Error should reference 'active' field");
        assertTrue(error.getFields().contains("mandatory"), "Error should reference 'mandatory' field");
    }

    @Test
    public void testNullActiveAndMandatory_ShouldBeInvalid() {
        // null active with mandatory=true should trigger validation error
        LanguageEntity language = new LanguageEntity("it");
        language.setActive(null);
        language.setMandatory(true);
        
        List<Message> errors = validator.validate(language);
        
        assertFalse(errors.isEmpty(), "Null active with mandatory=true should be invalid");
        assertEquals(1, errors.size(), "Should have exactly one validation error");

        Message error = errors.get(0);
        assertEquals(Message.MessageType.ERROR, error.getType(), "Should be an ERROR message");
        assertEquals("common.errors.language.mandatoryMustBeActive", error.getMessageKey(),
                "Error message key should be correct");
        assertNotNull(error.getFields(), "Error should have fields specified");
        assertTrue(error.getFields().contains("active"), "Error should reference 'active' field");
        assertTrue(error.getFields().contains("mandatory"), "Error should reference 'mandatory' field");
    }

    @Test
    public void testNullMandatory_ShouldBeValid() {
        // active=false with null mandatory should not trigger validation error
        LanguageEntity language = new LanguageEntity("pt");
        language.setActive(false);
        language.setMandatory(null);
        
        List<Message> errors = validator.validate(language);
        
        assertTrue(errors.isEmpty(), "Null mandatory flag should not trigger validation error");
    }

    @Test
    public void testBothNull_ShouldBeValid() {
        // Both null should be valid
        LanguageEntity language = new LanguageEntity("nl");
        language.setActive(null);
        language.setMandatory(null);
        
        List<Message> errors = validator.validate(language);
        
        assertTrue(errors.isEmpty(), "Both null flags should be valid");
    }

    @Test
    public void testNullEntity_ShouldNotThrowException() {
        // Null entity should not cause an exception
        assertDoesNotThrow(() -> {
            List<Message> errors = validator.validate(null);
            assertNotNull(errors, "Should return a non-null list even for null entity");
        }, "Validation should handle null entity gracefully");
    }

    @Test
    public void testValidationErrorMessageDetails() {
        // Verify the exact error message structure
        LanguageEntity language = new LanguageEntity("zh");
        language.setActive(false);
        language.setMandatory(true);
        
        List<Message> errors = validator.validate(language);
        
        assertFalse(errors.isEmpty());
        Message error = errors.get(0);
        
        assertNotNull(error.getMessageKey(), "Error should have a message key");
        assertEquals("common.errors.language.mandatoryMustBeActive", 
                error.getMessageKey(), "Error message key should match expected value");
        assertEquals(2, error.getFields().size(), "Error should reference exactly 2 fields");
    }

    @Test
    public void testMultipleLanguagesWithDifferentCombinations() {
        // Test multiple scenarios to ensure consistency
        LanguageEntity valid1 = new LanguageEntity("en");
        valid1.setActive(true);
        valid1.setMandatory(true);
        
        LanguageEntity valid2 = new LanguageEntity("de");
        valid2.setActive(true);
        valid2.setMandatory(false);
        
        LanguageEntity valid3 = new LanguageEntity("fr");
        valid3.setActive(false);
        valid3.setMandatory(false);
        
        LanguageEntity invalid = new LanguageEntity("es");
        invalid.setActive(false);
        invalid.setMandatory(true);
        
        assertTrue(validator.validate(valid1).isEmpty(), "Valid combination 1 should pass");
        assertTrue(validator.validate(valid2).isEmpty(), "Valid combination 2 should pass");
        assertTrue(validator.validate(valid3).isEmpty(), "Valid combination 3 should pass");
        assertFalse(validator.validate(invalid).isEmpty(), "Invalid combination should fail");
    }
}
