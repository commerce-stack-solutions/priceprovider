package io.commercestacksolutions.commons.permissionselector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelectorParserTest {

    private final SelectorParser parser = new SelectorParser();

    @Test
    void testParseSimpleEquals() {
        SelectorExpression expr = parser.parse("currencyRef == 'EUR'");

        assertTrue(expr.isLeaf());
        assertFalse(expr.isNegated());

        SelectorCondition condition = expr.getCondition();
        assertEquals("currencyRef", condition.getField());
        assertEquals(SelectorOperator.EQUALS, condition.getOperator());
        assertEquals("EUR", condition.getValue());
    }

    @Test
    void testParseSimpleNotEquals() {
        SelectorExpression expr = parser.parse("priceType != 'PURCHASE_PRICE'");

        assertTrue(expr.isLeaf());
        SelectorCondition condition = expr.getCondition();
        assertEquals("priceType", condition.getField());
        assertEquals(SelectorOperator.NOT_EQUALS, condition.getOperator());
        assertEquals("PURCHASE_PRICE", condition.getValue());
    }

    @Test
    void testParseHasAny() {
        SelectorExpression expr = parser.parse("channelRefs hasAny('channel-a','channel-b','channel-c')");

        assertTrue(expr.isLeaf());
        SelectorCondition condition = expr.getCondition();
        assertEquals("channelRefs", condition.getField());
        assertEquals(SelectorOperator.HAS_ANY, condition.getOperator());
        assertEquals(3, condition.getValues().size());
        assertTrue(condition.getValues().contains("channel-a"));
        assertTrue(condition.getValues().contains("channel-b"));
        assertTrue(condition.getValues().contains("channel-c"));
    }

    @Test
    void testParseHasAll() {
        SelectorExpression expr = parser.parse("groupRefs hasAll('group-1','group-2')");

        assertTrue(expr.isLeaf());
        SelectorCondition condition = expr.getCondition();
        assertEquals("groupRefs", condition.getField());
        assertEquals(SelectorOperator.HAS_ALL, condition.getOperator());
        assertEquals(2, condition.getValues().size());
    }

    @Test
    void testParseIsEmpty() {
        SelectorExpression expr = parser.parse("groupRefs isEmpty");

        assertTrue(expr.isLeaf());
        SelectorCondition condition = expr.getCondition();
        assertEquals("groupRefs", condition.getField());
        assertEquals(SelectorOperator.IS_EMPTY, condition.getOperator());
        assertTrue(condition.getValues().isEmpty());
    }

    @Test
    void testParseAndExpression() {
        SelectorExpression expr = parser.parse("currencyRef == 'EUR' AND priceType == 'SALES_PRICE'");

        assertFalse(expr.isLeaf());
        assertFalse(expr.isNegated());
        assertEquals(SelectorExpression.LogicalOperator.AND, expr.getLogicalOperator());
        assertEquals(2, expr.getChildren().size());

        SelectorExpression left = expr.getChildren().get(0);
        assertTrue(left.isLeaf());
        assertEquals("currencyRef", left.getCondition().getField());

        SelectorExpression right = expr.getChildren().get(1);
        assertTrue(right.isLeaf());
        assertEquals("priceType", right.getCondition().getField());
    }

    @Test
    void testParseOrExpression() {
        SelectorExpression expr = parser.parse("currencyRef == 'EUR' OR currencyRef == 'USD'");

        assertFalse(expr.isLeaf());
        assertEquals(SelectorExpression.LogicalOperator.OR, expr.getLogicalOperator());
        assertEquals(2, expr.getChildren().size());
    }

    @Test
    void testParseNotExpression() {
        SelectorExpression expr = parser.parse("NOT priceType == 'PURCHASE_PRICE'");

        assertTrue(expr.isLeaf());
        assertTrue(expr.isNegated());
        assertEquals("priceType", expr.getCondition().getField());
    }

    @Test
    void testParseGroupedExpression() {
        SelectorExpression expr = parser.parse("(currencyRef == 'EUR' OR currencyRef == 'USD') AND priceType == 'SALES_PRICE'");

        assertFalse(expr.isLeaf());
        assertEquals(SelectorExpression.LogicalOperator.AND, expr.getLogicalOperator());
        assertEquals(2, expr.getChildren().size());

        // First child should be the OR expression
        SelectorExpression left = expr.getChildren().get(0);
        assertFalse(left.isLeaf());
        assertEquals(SelectorExpression.LogicalOperator.OR, left.getLogicalOperator());
    }

    @Test
    void testParseComplexExpression() {
        String selector = "channelRefs hasAll('euro-sales-channel','dach-sales-channel') " +
                "AND priceType == 'SALES_PRICE' " +
                "AND currencyRef == 'EUR'";

        SelectorExpression expr = parser.parse(selector);

        assertFalse(expr.isLeaf());
        assertEquals(SelectorExpression.LogicalOperator.AND, expr.getLogicalOperator());
        assertEquals(3, expr.getChildren().size());
    }

    @Test
    void testParseNotGroupedExpression() {
        SelectorExpression expr = parser.parse("NOT (channelRefs hasAny('a','b') OR channelRefs hasAny('c','d'))");

        assertFalse(expr.isLeaf());
        assertTrue(expr.isNegated());
        assertEquals(SelectorExpression.LogicalOperator.OR, expr.getLogicalOperator());
    }

    @Test
    void testParseWithExtraSpaces() {
        SelectorExpression expr = parser.parse("  currencyRef   ==   'EUR'  ");

        assertTrue(expr.isLeaf());
        assertEquals("currencyRef", expr.getCondition().getField());
    }

    @Test
    void testParseEmptyStringShouldFail() {
        assertThrows(SelectorParseException.class, () -> parser.parse(""));
    }

    @Test
    void testParseInvalidSyntaxShouldFail() {
        assertThrows(SelectorParseException.class, () -> parser.parse("currencyRef =="));
    }

    @Test
    void testParseUnmatchedParenShouldFail() {
        assertThrows(SelectorParseException.class, () -> parser.parse("(currencyRef == 'EUR'"));
    }

    @Test
    void testParseInvalidOperatorShouldFail() {
        assertThrows(SelectorParseException.class, () -> parser.parse("currencyRef > 'EUR'"));
    }

    @Test
    void testParseMissingValueShouldFail() {
        assertThrows(SelectorParseException.class, () -> parser.parse("channelRefs hasAny()"));
    }

    @Test
    void testOperatorPrecedence() {
        // AND has higher precedence than OR
        // "a OR b AND c" should parse as "a OR (b AND c)"
        SelectorExpression expr = parser.parse("field1 == 'a' OR field2 == 'b' AND field3 == 'c'");

        assertFalse(expr.isLeaf());
        assertEquals(SelectorExpression.LogicalOperator.OR, expr.getLogicalOperator());

        // Right child should be the AND expression
        SelectorExpression right = expr.getChildren().get(1);
        assertFalse(right.isLeaf());
        assertEquals(SelectorExpression.LogicalOperator.AND, right.getLogicalOperator());
    }
}
