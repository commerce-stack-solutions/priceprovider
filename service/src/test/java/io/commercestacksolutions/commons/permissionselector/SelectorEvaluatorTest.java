package io.commercestacksolutions.commons.permissionselector;

import io.commercestacksolutions.commons.dataaccess.ReferenceKey;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SelectorEvaluatorTest {

    private final SelectorParser parser = new SelectorParser();
    private final SelectorEvaluator evaluator = new SelectorEvaluator();

    // Test entity classes
    static class TestPriceRow {
        private String currencyRef;
        private PriceType priceType;
        private Set<TestChannel> channelRefs = new HashSet<>();
        private Set<TestGroup> groupRefs = new HashSet<>();
        private boolean taxIncluded;

        public String getCurrencyRef() {
            return currencyRef;
        }

        public void setCurrencyRef(String currencyRef) {
            this.currencyRef = currencyRef;
        }

        public PriceType getPriceType() {
            return priceType;
        }

        public void setPriceType(PriceType priceType) {
            this.priceType = priceType;
        }

        public Set<TestChannel> getChannelRefs() {
            return channelRefs;
        }

        public boolean isTaxIncluded() {
            return taxIncluded;
        }

        public void setTaxIncluded(boolean taxIncluded) {
            this.taxIncluded = taxIncluded;
        }

        public Set<TestGroup> getGroupRefs() {
            return groupRefs;
        }
    }

    enum PriceType {
        SALES_PRICE, PURCHASE_PRICE, RENTAL_BASE_PRICE, RENTAL_DAILY_RATE
    }

    static class TestChannel {
        @Id
        private String id;

        public TestChannel(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    static class TestGroup {
        @Id
        private String id;

        @ReferenceKey
        private String path;

        public TestGroup(String id, String path) {
            this.id = id;
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    @Test
    void testEvaluateSimpleEquals() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setCurrencyRef("EUR");

        SelectorExpression expr = parser.parse("currencyRef == 'EUR'");
        assertTrue(evaluator.evaluate(expr, priceRow));

        SelectorExpression expr2 = parser.parse("currencyRef == 'USD'");
        assertFalse(evaluator.evaluate(expr2, priceRow));
    }

    @Test
    void testEvaluateNotEquals() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setCurrencyRef("EUR");

        SelectorExpression expr = parser.parse("currencyRef != 'USD'");
        assertTrue(evaluator.evaluate(expr, priceRow));

        SelectorExpression expr2 = parser.parse("currencyRef != 'EUR'");
        assertFalse(evaluator.evaluate(expr2, priceRow));
    }

    @Test
    void testEvaluateEnumEquals() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setPriceType(PriceType.SALES_PRICE);

        SelectorExpression expr = parser.parse("priceType == 'SALES_PRICE'");
        assertTrue(evaluator.evaluate(expr, priceRow));

        SelectorExpression expr2 = parser.parse("priceType == 'PURCHASE_PRICE'");
        assertFalse(evaluator.evaluate(expr2, priceRow));
    }

    @Test
    void testEvaluateBooleanEquals() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setTaxIncluded(true);

        SelectorExpression expr = parser.parse("taxIncluded == 'true'");
        assertTrue(evaluator.evaluate(expr, priceRow));
    }

    @Test
    void testEvaluateHasAnyOnCollection() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.getChannelRefs().add(new TestChannel("channel-a"));
        priceRow.getChannelRefs().add(new TestChannel("channel-b"));

        SelectorExpression expr = parser.parse("channelRefs hasAny('channel-a','channel-c')");
        assertTrue(evaluator.evaluate(expr, priceRow)); // has channel-a

        SelectorExpression expr2 = parser.parse("channelRefs hasAny('channel-c','channel-d')");
        assertFalse(evaluator.evaluate(expr2, priceRow)); // has neither
    }

    @Test
    void testEvaluateHasAnyWithReferenceKey() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.getGroupRefs().add(new TestGroup("id1", "group-a"));
        priceRow.getGroupRefs().add(new TestGroup("id2", "group-b"));

        // Should match using @ReferenceKey (path), not @Id
        SelectorExpression expr = parser.parse("groupRefs hasAny('group-a','group-c')");
        assertTrue(evaluator.evaluate(expr, priceRow));

        SelectorExpression expr2 = parser.parse("groupRefs hasAny('group-c','group-d')");
        assertFalse(evaluator.evaluate(expr2, priceRow));
    }

    @Test
    void testEvaluateHasAllOnCollection() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.getChannelRefs().add(new TestChannel("channel-a"));
        priceRow.getChannelRefs().add(new TestChannel("channel-b"));
        priceRow.getChannelRefs().add(new TestChannel("channel-c"));

        SelectorExpression expr = parser.parse("channelRefs hasAll('channel-a','channel-b')");
        assertTrue(evaluator.evaluate(expr, priceRow)); // has both

        SelectorExpression expr2 = parser.parse("channelRefs hasAll('channel-a','channel-d')");
        assertFalse(evaluator.evaluate(expr2, priceRow)); // missing channel-d
    }

    @Test
    void testEvaluateIsEmptyOnString() {
        TestPriceRow priceRow1 = new TestPriceRow();
        priceRow1.setCurrencyRef(null);

        SelectorExpression expr = parser.parse("currencyRef isEmpty");
        assertTrue(evaluator.evaluate(expr, priceRow1));

        TestPriceRow priceRow2 = new TestPriceRow();
        priceRow2.setCurrencyRef("");

        assertTrue(evaluator.evaluate(expr, priceRow2));

        TestPriceRow priceRow3 = new TestPriceRow();
        priceRow3.setCurrencyRef("EUR");

        assertFalse(evaluator.evaluate(expr, priceRow3));
    }

    @Test
    void testEvaluateIsEmptyOnCollection() {
        TestPriceRow priceRow1 = new TestPriceRow();
        // channelRefs is empty by default

        SelectorExpression expr = parser.parse("channelRefs isEmpty");
        assertTrue(evaluator.evaluate(expr, priceRow1));

        TestPriceRow priceRow2 = new TestPriceRow();
        priceRow2.getChannelRefs().add(new TestChannel("channel-a"));

        assertFalse(evaluator.evaluate(expr, priceRow2));
    }

    @Test
    void testEvaluateAndExpression() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setCurrencyRef("EUR");
        priceRow.setPriceType(PriceType.SALES_PRICE);

        SelectorExpression expr = parser.parse("currencyRef == 'EUR' AND priceType == 'SALES_PRICE'");
        assertTrue(evaluator.evaluate(expr, priceRow));

        SelectorExpression expr2 = parser.parse("currencyRef == 'EUR' AND priceType == 'PURCHASE_PRICE'");
        assertFalse(evaluator.evaluate(expr2, priceRow));
    }

    @Test
    void testEvaluateOrExpression() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setCurrencyRef("EUR");

        SelectorExpression expr = parser.parse("currencyRef == 'EUR' OR currencyRef == 'USD'");
        assertTrue(evaluator.evaluate(expr, priceRow));

        SelectorExpression expr2 = parser.parse("currencyRef == 'USD' OR currencyRef == 'GBP'");
        assertFalse(evaluator.evaluate(expr2, priceRow));
    }

    @Test
    void testEvaluateNotExpression() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setPriceType(PriceType.SALES_PRICE);

        SelectorExpression expr = parser.parse("NOT priceType == 'PURCHASE_PRICE'");
        assertTrue(evaluator.evaluate(expr, priceRow));

        SelectorExpression expr2 = parser.parse("NOT priceType == 'SALES_PRICE'");
        assertFalse(evaluator.evaluate(expr2, priceRow));
    }

    @Test
    void testEvaluateComplexExpression() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setCurrencyRef("EUR");
        priceRow.setPriceType(PriceType.SALES_PRICE);
        priceRow.getChannelRefs().add(new TestChannel("euro-sales-channel"));
        priceRow.getChannelRefs().add(new TestChannel("dach-sales-channel"));

        String selector = "channelRefs hasAll('euro-sales-channel','dach-sales-channel') " +
                "AND priceType == 'SALES_PRICE' " +
                "AND currencyRef == 'EUR'";

        SelectorExpression expr = parser.parse(selector);
        assertTrue(evaluator.evaluate(expr, priceRow));
    }

    @Test
    void testEvaluateGroupedExpression() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setCurrencyRef("EUR");
        priceRow.setPriceType(PriceType.SALES_PRICE);

        SelectorExpression expr = parser.parse("(currencyRef == 'EUR' OR currencyRef == 'USD') AND priceType == 'SALES_PRICE'");
        assertTrue(evaluator.evaluate(expr, priceRow));
    }

    @Test
    void testEvaluateNullField() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setCurrencyRef(null);

        SelectorExpression expr = parser.parse("currencyRef == 'EUR'");
        assertFalse(evaluator.evaluate(expr, priceRow));
    }

    @Test
    void testEvaluateInvalidFieldShouldFail() {
        TestPriceRow priceRow = new TestPriceRow();

        SelectorExpression expr = parser.parse("nonExistentField == 'value'");
        assertThrows(SelectorEvaluationException.class, () -> evaluator.evaluate(expr, priceRow));
    }

    @Test
    void testCaseSensitivity() {
        TestPriceRow priceRow = new TestPriceRow();
        priceRow.setCurrencyRef("EUR");

        // Exact match expected (case-sensitive)
        SelectorExpression expr1 = parser.parse("currencyRef == 'EUR'");
        assertTrue(evaluator.evaluate(expr1, priceRow));

        SelectorExpression expr2 = parser.parse("currencyRef == 'eur'");
        assertFalse(evaluator.evaluate(expr2, priceRow));
    }
}
