package io.commercestacksolutions.priceproviderservice.service.approle.validation;

import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppPermissionSelectorValidationRule.
 */
class AppPermissionSelectorValidationRuleTest {

    private final AppPermissionSelectorValidationRule validationRule = new AppPermissionSelectorValidationRule();

    @Test
    void testValidPermissionWithoutSelector() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow:read");

        List<Message> errors = validationRule.validate(permission);

        assertTrue(errors.isEmpty(), "Valid permission without selector should have no errors");
    }

    @Test
    void testValidPermissionWithSelector() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow[currencyRef=='EUR']:read");

        List<Message> errors = validationRule.validate(permission);

        assertTrue(errors.isEmpty(), "Valid permission with selector should have no errors");
    }

    @Test
    void testValidPermissionWithComplexSelector() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow[currencyRef=='EUR' AND priceType=='SALES_PRICE']:write");

        List<Message> errors = validationRule.validate(permission);

        assertTrue(errors.isEmpty(), "Valid permission with complex selector should have no errors");
    }

    @Test
    void testValidPermissionWithNotEqualsOperator() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow[priceType!='PURCHASE_PRICE']:read");

        List<Message> errors = validationRule.validate(permission);

        assertTrue(errors.isEmpty(), "Valid permission with NOT_EQUALS operator should have no errors");
    }

    @Test
    void testValidPermissionWithHasAny() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow[channelRefs hasAny('global-b2b-sales-channel')]:read");

        List<Message> errors = validationRule.validate(permission);

        assertTrue(errors.isEmpty(), "Valid permission with hasAny should have no errors");
    }

    @Test
    void testInvalidPermissionFormat() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("invalid-permission-name");

        List<Message> errors = validationRule.validate(permission);

        assertFalse(errors.isEmpty(), "Invalid permission format should have errors");
        assertEquals(1, errors.size());
        assertEquals(Message.MessageType.ERROR, errors.get(0).getType());
        assertTrue(errors.get(0).getMessageKey().contains("Invalid permission name or selector syntax"));
        assertTrue(errors.get(0).getFields().contains("name"));
    }

    @Test
    void testInvalidSelectorSyntax() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow[currencyRef=='EUR' AND]:read");

        List<Message> errors = validationRule.validate(permission);

        assertFalse(errors.isEmpty(), "Invalid selector syntax should have errors");
        assertEquals(1, errors.size());
        assertEquals(Message.MessageType.ERROR, errors.get(0).getType());
        assertTrue(errors.get(0).getMessageKey().contains("Invalid permission name or selector syntax"));
        assertTrue(errors.get(0).getFields().contains("name"));
    }

    @Test
    void testMalformedSelector() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow[currencyRef=]:read");

        List<Message> errors = validationRule.validate(permission);

        assertFalse(errors.isEmpty(), "Malformed selector should have errors");
        assertEquals(1, errors.size());
        assertEquals(Message.MessageType.ERROR, errors.get(0).getType());
    }

    @Test
    void testUnmatchedParentheses() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow[(currencyRef=='EUR']:read");

        List<Message> errors = validationRule.validate(permission);

        assertFalse(errors.isEmpty(), "Unmatched parentheses should have errors");
        assertEquals(1, errors.size());
        assertEquals(Message.MessageType.ERROR, errors.get(0).getType());
    }

    @Test
    void testNullPermission() {
        List<Message> errors = validationRule.validate(null);

        assertFalse(errors.isEmpty(), "Null permission should have errors");
        assertEquals(1, errors.size());
        assertEquals(Message.MessageType.ERROR, errors.get(0).getType());
    }

    @Test
    void testEmptyPermissionName() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("");

        List<Message> errors = validationRule.validate(permission);

        // Empty names are handled by @MandatoryField validation
        assertTrue(errors.isEmpty(), "Empty permission name should be handled by @MandatoryField");
    }

    @Test
    void testNullPermissionName() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName(null);

        List<Message> errors = validationRule.validate(permission);

        // Null names are handled by @MandatoryField validation
        assertTrue(errors.isEmpty(), "Null permission name should be handled by @MandatoryField");
    }

    @Test
    void testValidPermissionWithFlexiblePrefix() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.public:PriceRow[groupRefs isEmpty]:read");

        List<Message> errors = validationRule.validate(permission);

        assertTrue(errors.isEmpty(), "Valid permission with priceprovider.public prefix should have no errors");
    }

    @Test
    void testValidPermissionWithCustomPrefix() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("custom.app:Resource:read");

        List<Message> errors = validationRule.validate(permission);

        assertTrue(errors.isEmpty(), "Valid permission with custom prefix should have no errors");
    }

    @Test
    void testInvalidSelectorWithWrongOperator() {
        AppPermissionEntity permission = new AppPermissionEntity();
        permission.setName("priceprovider.admin:PriceRow[currencyRef==='EUR']:read");

        List<Message> errors = validationRule.validate(permission);

        assertFalse(errors.isEmpty(), "Invalid operator === should have errors");
        assertEquals(1, errors.size());
    }
}
