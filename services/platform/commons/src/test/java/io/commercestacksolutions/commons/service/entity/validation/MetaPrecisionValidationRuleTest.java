package io.commercestacksolutions.commons.service.entity.validation;

import io.commercestacksolutions.commons.dataaccess.meta.MetaPrecision;
import io.commercestacksolutions.commons.web.rest.Message;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetaPrecisionValidationRuleTest {

    private final MetaPrecisionValidationRule rule = new MetaPrecisionValidationRule();

    static class TestEntity {
        @MetaPrecision(2)
        private BigDecimal value1;

        @MetaPrecision(4)
        private BigDecimal value2;

        private BigDecimal unannotated;

        public TestEntity(BigDecimal value1, BigDecimal value2, BigDecimal unannotated) {
            this.value1 = value1;
            this.value2 = value2;
            this.unannotated = unannotated;
        }
    }

    @Test
    void testValidPrecision() {
        TestEntity entity = new TestEntity(
                new BigDecimal("10.50"),
                new BigDecimal("123.4567"),
                new BigDecimal("1.123456789")
        );
        List<Message> errors = rule.validate(entity);
        assertTrue(errors.isEmpty(), "Expected no validation errors");
    }

    @Test
    void testValidPrecisionWithTrailingZeros() {
        TestEntity entity = new TestEntity(
                new BigDecimal("10.50000"), // Strips to 10.5 (scale 1 <= 2)
                new BigDecimal("123.456700"), // Strips to 123.4567 (scale 4 <= 4)
                null
        );
        List<Message> errors = rule.validate(entity);
        assertTrue(errors.isEmpty(), "Expected no validation errors with trailing zeros");
    }

    @Test
    void testInvalidPrecision() {
        TestEntity entity = new TestEntity(
                new BigDecimal("10.555"), // scale 3 > 2 (FAIL)
                new BigDecimal("123.4567"), // scale 4 <= 4 (PASS)
                null
        );
        List<Message> errors = rule.validate(entity);
        assertEquals(1, errors.size());
        assertEquals("Field 'value1' exceeds allowed precision of 2 decimal places", errors.get(0).getMessageKey());
        assertEquals(List.of("value1"), errors.get(0).getFields());
    }

    @Test
    void testNullValues() {
        TestEntity entity = new TestEntity(null, null, null);
        List<Message> errors = rule.validate(entity);
        assertTrue(errors.isEmpty(), "Expected no validation errors for null values");
    }
}
