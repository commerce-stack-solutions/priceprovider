package io.commercestacksolutions.commons.permissionselector;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionMatcherTest {

    private final PermissionMatcher permissionMatcher = new PermissionMatcher();

    // Test entity class
    static class TestPriceRow {
        private String currencyRef;
        private String priceType;

        public TestPriceRow(String currencyRef, String priceType) {
            this.currencyRef = currencyRef;
            this.priceType = priceType;
        }

        public String getCurrencyRef() {
            return currencyRef;
        }

        public String getPriceType() {
            return priceType;
        }
    }

    @Test
    void testHasAccessWithGlobalPermission() {
        AppPermissionEntity perm = createPermission("priceprovider.admin:PriceRow:read");
        TestPriceRow priceRow = new TestPriceRow("EUR", "SALES_PRICE");

        boolean hasAccess = permissionMatcher.hasAccess(Collections.singleton(perm), "PriceRow", "read", priceRow);

        assertTrue(hasAccess, "Global permission should grant access to any object");
    }

    @Test
    void testHasAccessWithMatchingSelector() {
        AppPermissionEntity perm = createPermission("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");
        TestPriceRow priceRow = new TestPriceRow("EUR", "SALES_PRICE");

        boolean hasAccess = permissionMatcher.hasAccess(Collections.singleton(perm), "PriceRow", "read", priceRow);

        assertTrue(hasAccess, "Permission with matching selector should grant access");
    }

    @Test
    void testHasAccessWithNonMatchingSelector() {
        AppPermissionEntity perm = createPermission("priceprovider.admin:PriceRow[currencyRef=='USD']:read");
        TestPriceRow priceRow = new TestPriceRow("EUR", "SALES_PRICE");

        boolean hasAccess = permissionMatcher.hasAccess(Collections.singleton(perm), "PriceRow", "read", priceRow);

        assertFalse(hasAccess, "Permission with non-matching selector should deny access");
    }

    @Test
    void testHasAccessWithMultiplePermissionsUnion() {
        AppPermissionEntity perm1 = createPermission("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");
        AppPermissionEntity perm2 = createPermission("priceprovider.admin:PriceRow[currencyRef=='USD']:read");

        TestPriceRow priceRowEUR = new TestPriceRow("EUR", "SALES_PRICE");
        TestPriceRow priceRowUSD = new TestPriceRow("USD", "SALES_PRICE");
        TestPriceRow priceRowGBP = new TestPriceRow("GBP", "SALES_PRICE");

        Set<AppPermissionEntity> permissions = new HashSet<>(Arrays.asList(perm1, perm2));

        assertTrue(permissionMatcher.hasAccess(permissions, "PriceRow", "read", priceRowEUR),
                "EUR price row should be accessible (matches first permission)");
        assertTrue(permissionMatcher.hasAccess(permissions, "PriceRow", "read", priceRowUSD),
                "USD price row should be accessible (matches second permission)");
        assertFalse(permissionMatcher.hasAccess(permissions, "PriceRow", "read", priceRowGBP),
                "GBP price row should not be accessible (no matching permission)");
    }

    @Test
    void testHasAccessWithComplexSelector() {
        AppPermissionEntity perm = createPermission(
                "priceprovider.admin:PriceRow[currencyRef=='EUR' AND priceType=='SALES_PRICE']:read");

        TestPriceRow matchingRow = new TestPriceRow("EUR", "SALES_PRICE");
        TestPriceRow nonMatchingRow1 = new TestPriceRow("USD", "SALES_PRICE");
        TestPriceRow nonMatchingRow2 = new TestPriceRow("EUR", "PURCHASE_PRICE");

        assertTrue(permissionMatcher.hasAccess(Collections.singleton(perm), "PriceRow", "read", matchingRow),
                "Row matching both conditions should be accessible");
        assertFalse(permissionMatcher.hasAccess(Collections.singleton(perm), "PriceRow", "read", nonMatchingRow1),
                "Row with wrong currency should not be accessible");
        assertFalse(permissionMatcher.hasAccess(Collections.singleton(perm), "PriceRow", "read", nonMatchingRow2),
                "Row with wrong price type should not be accessible");
    }

    @Test
    void testHasAccessWithWrongAction() {
        AppPermissionEntity perm = createPermission("priceprovider.admin:PriceRow:read");
        TestPriceRow priceRow = new TestPriceRow("EUR", "SALES_PRICE");

        boolean hasAccess = permissionMatcher.hasAccess(Collections.singleton(perm), "PriceRow", "write", priceRow);

        assertFalse(hasAccess, "Read permission should not grant write access");
    }

    @Test
    void testHasAccessWithWrongDataType() {
        AppPermissionEntity perm = createPermission("priceprovider.admin:Channel:read");
        TestPriceRow priceRow = new TestPriceRow("EUR", "SALES_PRICE");

        boolean hasAccess = permissionMatcher.hasAccess(Collections.singleton(perm), "PriceRow", "read", priceRow);

        assertFalse(hasAccess, "Channel permission should not grant access to PriceRow");
    }

    @Test
    void testHasAccessWithNoPermissions() {
        TestPriceRow priceRow = new TestPriceRow("EUR", "SALES_PRICE");

        boolean hasAccess = permissionMatcher.hasAccess(Collections.emptySet(), "PriceRow", "read", priceRow);

        assertFalse(hasAccess, "No permissions should deny access");
    }

    @Test
    void testHasAnyPermission() {
        AppPermissionEntity perm = createPermission("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");

        assertTrue(permissionMatcher.hasAnyPermission(Collections.singleton(perm), "PriceRow", "read"));
        assertFalse(permissionMatcher.hasAnyPermission(Collections.singleton(perm), "PriceRow", "write"));
        assertFalse(permissionMatcher.hasAnyPermission(Collections.singleton(perm), "Channel", "read"));
    }

    @Test
    void testHasGlobalPermission() {
        AppPermissionEntity globalPerm = createPermission("priceprovider.admin:PriceRow:read");
        AppPermissionEntity selectorPerm = createPermission("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");

        assertTrue(permissionMatcher.hasGlobalPermission(Collections.singleton(globalPerm), "PriceRow", "read"),
                "Should detect global permission");
        assertFalse(permissionMatcher.hasGlobalPermission(Collections.singleton(selectorPerm), "PriceRow", "read"),
                "Should not treat selector-based permission as global");
    }

    @Test
    void testGetPermissionsFor() {
        AppPermissionEntity perm1 = createPermission("priceprovider.admin:PriceRow:read");
        AppPermissionEntity perm2 = createPermission("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");
        AppPermissionEntity perm3 = createPermission("priceprovider.admin:PriceRow:write");
        AppPermissionEntity perm4 = createPermission("priceprovider.admin:Channel:read");

        Set<AppPermissionEntity> permissions = new HashSet<>(Arrays.asList(perm1, perm2, perm3, perm4));

        var readPermissions = permissionMatcher.getPermissionsFor(permissions, "PriceRow", "read");

        assertEquals(2, readPermissions.size(), "Should find 2 PriceRow:read permissions");
        assertTrue(readPermissions.stream().anyMatch(p -> !p.hasSelector()), "Should include global permission");
        assertTrue(readPermissions.stream().anyMatch(p -> p.hasSelector()), "Should include selector permission");
    }

    // Helper method to create a permission entity
    private AppPermissionEntity createPermission(String name) {
        AppPermissionEntity entity = new AppPermissionEntity();
        entity.setName(name);
        return entity;
    }
}
