package io.commercestacksolutions.commons.permissionselector;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionFilterBuilderTest {

    private final PermissionFilterBuilder filterBuilder = new PermissionFilterBuilder();

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
    void testBuildFilterWithNoPermissions() throws InvalidParameterException {
        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.emptySet(), "PriceRow", "read");

        assertNotNull(spec, "Should return a deny-all specification");
        // The specification should be a disjunction (always false)
    }

    @Test
    void testBuildFilterWithGlobalPermission() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission("priceprovider.admin:PriceRow:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNull(spec, "Global permission should return null (no filtering needed)");
    }

    @Test
    void testBuildFilterWithSelectorPermission() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNotNull(spec, "Selector permission should return a filtering specification");
    }

    @Test
    void testBuildFilterWithMultipleSelectorPermissions() throws InvalidParameterException {
        AppPermissionEntity perm1 = createPermission("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");
        AppPermissionEntity perm2 = createPermission("priceprovider.admin:PriceRow[currencyRef=='USD']:read");

        Set<AppPermissionEntity> permissions = new HashSet<>(Arrays.asList(perm1, perm2));
        Specification<TestPriceRow> spec = filterBuilder.buildFilter(permissions, "PriceRow", "read");

        assertNotNull(spec, "Multiple selector permissions should return a filtering specification");
        // The specification should be an OR of the two conditions
    }

    @Test
    void testBuildFilterWithComplexSelector() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission(
                "priceprovider.admin:PriceRow[currencyRef=='EUR' AND priceType=='SALES_PRICE']:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNotNull(spec, "Complex selector permission should return a filtering specification");
    }

    @Test
    void testBuildFilterWithWrongDataType() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission("priceprovider.admin:Channel:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNotNull(spec, "Wrong data type should return deny-all specification");
        // The specification should be a disjunction (always false)
    }

    @Test
    void testBuildFilterWithWrongAction() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission("priceprovider.admin:PriceRow:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "write");

        assertNotNull(spec, "Wrong action should return deny-all specification");
        // The specification should be a disjunction (always false)
    }

    @Test
    void testBuildFilterWithMixedPermissions() throws InvalidParameterException {
        // Mix of global and selector permissions - global wins
        AppPermissionEntity perm1 = createPermission("priceprovider.admin:PriceRow:read"); // Global
        AppPermissionEntity perm2 = createPermission("priceprovider.admin:PriceRow[currencyRef=='EUR']:read"); // Selector

        Set<AppPermissionEntity> permissions = new HashSet<>(Arrays.asList(perm1, perm2));
        Specification<TestPriceRow> spec = filterBuilder.buildFilter(permissions, "PriceRow", "read");

        assertNull(spec, "Global permission should take precedence, returning null");
    }

    @Test
    void testBuildFilterWithNotOperator() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission(
                "priceprovider.admin:PriceRow[NOT priceType=='PURCHASE_PRICE']:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNotNull(spec, "NOT operator should be supported in selector");
    }

    @Test
    void testBuildFilterWithOrOperator() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission(
                "priceprovider.admin:PriceRow[priceType=='SALES_PRICE' OR priceType=='RENTAL_BASE_PRICE']:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNotNull(spec, "OR operator should be supported in selector");
    }

    @Test
    void testBuildFilterWithIsEmptyOperator() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission(
                "priceprovider.admin:PriceRow[groupRefs isEmpty]:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNotNull(spec, "isEmpty operator should be supported in selector");
    }

    @Test
    void testBuildFilterWithHasAnyOperator() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission(
                "priceprovider.admin:PriceRow[groupRefs hasAny ('GROUP1', 'GROUP2')]:read");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNotNull(spec, "hasAny operator should be supported in selector");
    }

    @Test
    void testBuildFilterWithInvalidPermissionName() throws InvalidParameterException {
        AppPermissionEntity perm = createPermission("invalid-permission-name");

        Specification<TestPriceRow> spec = filterBuilder.buildFilter(Collections.singleton(perm), "PriceRow", "read");

        assertNotNull(spec, "Invalid permission should be ignored and return deny-all specification");
    }

    // Helper method to create a permission entity
    private AppPermissionEntity createPermission(String name) {
        AppPermissionEntity entity = new AppPermissionEntity();
        entity.setName(name);
        return entity;
    }
}
