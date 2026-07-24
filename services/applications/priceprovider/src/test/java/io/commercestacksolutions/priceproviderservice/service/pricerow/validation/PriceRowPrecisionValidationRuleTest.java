package io.commercestacksolutions.priceproviderservice.service.pricerow.validation;

import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceRowPrecisionValidationRuleTest {

    private final PriceRowPrecisionValidationRule rule = new PriceRowPrecisionValidationRule();

    @Test
    void testValidPrecision() {
        PriceRowEntity entity = new PriceRowEntity();
        entity.setPriceValue(new BigDecimal("10.50"));

        List<Message> errors = rule.validate(entity);
        assertTrue(errors.isEmpty(), "Expected no validation errors");
    }

    @Test
    void testValidPrecisionWithTrailingZeros() {
        PriceRowEntity entity = new PriceRowEntity();
        entity.setPriceValue(new BigDecimal("10.50000")); // Strips to 10.5 (scale 1 <= 2)

        List<Message> errors = rule.validate(entity);
        assertTrue(errors.isEmpty(), "Expected no validation errors with trailing zeros");
    }

    @Test
    void testInvalidPrecision() {
        PriceRowEntity entity = new PriceRowEntity();
        entity.setPriceValue(new BigDecimal("10.555")); // scale 3 > 2 (FAIL)

        List<Message> errors = rule.validate(entity);
        assertEquals(1, errors.size());
        assertEquals("Field 'priceValue' exceeds allowed precision of 2 decimal places", errors.get(0).getMessageKey());
        assertEquals(List.of("priceValue"), errors.get(0).getFields());
    }

    @Test
    void testNullValues() {
        PriceRowEntity entity = new PriceRowEntity();
        entity.setPriceValue(null);

        List<Message> errors = rule.validate(entity);
        assertTrue(errors.isEmpty(), "Expected no validation errors for null value");
    }

    @Test
    void testNullEntity() {
        List<Message> errors = rule.validate(null);
        assertTrue(errors.isEmpty(), "Expected no validation errors for null entity");
    }
}
