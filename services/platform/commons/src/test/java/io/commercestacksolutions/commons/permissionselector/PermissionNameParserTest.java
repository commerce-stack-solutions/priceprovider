package io.commercestacksolutions.commons.permissionselector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionNameParserTest {

    private final PermissionNameParser parser = new PermissionNameParser();

    @Test
    void testParseGlobalPermission() {
        PermissionNameParser.ParsedPermission perm = parser.parse("priceprovider.admin:PriceRow:read");

        assertEquals("priceprovider.admin:PriceRow:read", perm.getFullName());
        assertEquals("PriceRow", perm.getDataType());
        assertEquals("read", perm.getAction());
        assertNull(perm.getSelector());
        assertFalse(perm.hasSelector());
    }

    @Test
    void testParseCapabilityPermission() {
        PermissionNameParser.ParsedPermission perm = parser.parse("priceprovider.admin:ServiceInitialization:write");

        assertEquals("ServiceInitialization", perm.getDataType());
        assertEquals("write", perm.getAction());
        assertFalse(perm.hasSelector());
    }

    @Test
    void testParsePermissionWithSelector() {
        PermissionNameParser.ParsedPermission perm = parser.parse("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");

        assertEquals("PriceRow", perm.getDataType());
        assertEquals("read", perm.getAction());
        assertNotNull(perm.getSelector());
        assertTrue(perm.hasSelector());

        // Verify the selector was parsed correctly
        assertTrue(perm.getSelector().isLeaf());
        assertEquals("currencyRef", perm.getSelector().getCondition().getField());
    }

    @Test
    void testParsePermissionWithComplexSelector() {
        String permName = "priceprovider.admin:PriceRow[channelRefs hasAll('a','b') AND priceType=='SALES_PRICE']:write";
        PermissionNameParser.ParsedPermission perm = parser.parse(permName);

        assertEquals("PriceRow", perm.getDataType());
        assertEquals("write", perm.getAction());
        assertTrue(perm.hasSelector());

        // Verify the selector was parsed as AND expression
        assertFalse(perm.getSelector().isLeaf());
        assertEquals(SelectorExpression.LogicalOperator.AND, perm.getSelector().getLogicalOperator());
    }

    @Test
    void testParseDeletePermission() {
        PermissionNameParser.ParsedPermission perm = parser.parse("priceprovider.admin:PriceRow:delete");

        assertEquals("PriceRow", perm.getDataType());
        assertEquals("delete", perm.getAction());
    }

    @Test
    void testMatchesTypeAndAction() {
        PermissionNameParser.ParsedPermission perm = parser.parse("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");

        assertTrue(perm.matchesTypeAndAction("PriceRow", "read"));
        assertFalse(perm.matchesTypeAndAction("PriceRow", "write"));
        assertFalse(perm.matchesTypeAndAction("Channel", "read"));
    }

    @Test
    void testIsValid() {
        assertTrue(parser.isValid("priceprovider.admin:PriceRow:read"));
        assertTrue(parser.isValid("priceprovider.admin:PriceRow[currencyRef=='EUR']:read"));
        assertFalse(parser.isValid("invalid:permission"));
        assertFalse(parser.isValid("priceprovider.admin:PriceRow"));
        assertFalse(parser.isValid(""));
    }

    @Test
    void testHasSelector() {
        assertTrue(parser.hasSelector("priceprovider.admin:PriceRow[currencyRef=='EUR']:read"));
        assertFalse(parser.hasSelector("priceprovider.admin:PriceRow:read"));
        assertFalse(parser.hasSelector("invalid:permission"));
    }

    @Test
    void testInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("invalid:format"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("priceprovider.admin:PriceRow"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    void testInvalidSelector() {
        // Invalid selector syntax should fail
        String invalidPermName = "priceprovider.admin:PriceRow[currencyRef ==]:read";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(invalidPermName));
    }

    @Test
    void testEmptySelector() {
        // Empty selector (just whitespace) should be treated as no selector
        PermissionNameParser.ParsedPermission perm = parser.parse("priceprovider.admin:PriceRow[ ]:read");
        assertFalse(perm.hasSelector());
    }

    @Test
    void testMultilineSelector() {
        // Test that selectors with newlines are handled
        String permName = "priceprovider.admin:PriceRow[currencyRef=='EUR'\nAND priceType=='SALES_PRICE']:read";

        // This should fail because the pattern doesn't support newlines in the selector
        // If we want to support multiline, we need to update the pattern or normalize the input
        assertThrows(IllegalArgumentException.class, () -> parser.parse(permName));
    }

    @Test
    void testCaseSensitiveAction() {
        // Actions are lowercase
        PermissionNameParser.ParsedPermission perm = parser.parse("priceprovider.admin:PriceRow:read");
        assertEquals("read", perm.getAction());

        // Uppercase action should fail
        assertThrows(IllegalArgumentException.class, () -> parser.parse("priceprovider.admin:PriceRow:Read"));
    }
}
