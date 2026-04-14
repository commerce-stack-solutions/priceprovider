package io.commercestacksolutions.commons.service.entity.validation.rules;

import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.dataaccess.meta.MetaInfoBuilder;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RequireMandatoryFieldsRule}.
 *
 * <p>Verifies the generic rule detects null/blank mandatory fields using
 * the {@link EntityMetaInfoRegistry} and that it works correctly for
 * concrete entities such as {@link GroupEntity} and {@link TaxClassEntity}.</p>
 */
class RequireMandatoryFieldsRuleTest {

    private EntityMetaInfoRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EntityMetaInfoRegistry();
        registry.register(GroupEntity.class,    MetaInfoBuilder.build(GroupEntity.class));
        registry.register(TaxClassEntity.class, MetaInfoBuilder.build(TaxClassEntity.class));
    }

    // -------------------------------------------------------------------------
    // GroupEntity tests
    // -------------------------------------------------------------------------

    @Test
    void validate_nullEntity_returnsNoErrors() {
        RequireMandatoryFieldsRule<GroupEntity> rule = new RequireMandatoryFieldsRule<>(GroupEntity.class, registry);
        assertTrue(rule.validate(null).isEmpty(), "Null entity must produce no validation errors");
    }

    @Test
    void validate_groupWithAllMandatoryFields_passes() {
        GroupEntity group = new GroupEntity("GRP-001");
        group.setName("Test Group");

        RequireMandatoryFieldsRule<GroupEntity> rule = new RequireMandatoryFieldsRule<>(GroupEntity.class, registry);
        assertTrue(rule.validate(group).isEmpty(), "Group with all mandatory fields must pass");
    }

    @Test
    void validate_groupWithNullId_returnsError() {
        GroupEntity group = new GroupEntity();
        // path is null (id is auto-generated UUID, not validated as mandatory)
        group.setName("Test Group");

        RequireMandatoryFieldsRule<GroupEntity> rule = new RequireMandatoryFieldsRule<>(GroupEntity.class, registry);
        List<Message> errors = rule.validate(group);

        assertFalse(errors.isEmpty(), "Missing path must produce a validation error");
        assertTrue(errors.stream().anyMatch(m -> m.getFields().contains("path")),
                "Error must reference the 'path' field");
    }

    @Test
    void validate_groupWithBlankName_returnsError() {
        GroupEntity group = new GroupEntity("GRP-001");
        group.setName("   "); // blank string

        RequireMandatoryFieldsRule<GroupEntity> rule = new RequireMandatoryFieldsRule<>(GroupEntity.class, registry);
        List<Message> errors = rule.validate(group);

        assertFalse(errors.isEmpty(), "Blank name must produce a validation error");
        assertTrue(errors.stream().anyMatch(m -> m.getFields().contains("name")),
                "Error must reference the 'name' field");
    }

    @Test
    void validate_groupWithNullName_returnsError() {
        GroupEntity group = new GroupEntity("GRP-001");
        // name is null

        RequireMandatoryFieldsRule<GroupEntity> rule = new RequireMandatoryFieldsRule<>(GroupEntity.class, registry);
        List<Message> errors = rule.validate(group);

        assertFalse(errors.isEmpty(), "Missing name must produce a validation error");
        assertTrue(errors.stream().anyMatch(m -> m.getFields().contains("name")),
                "Error must reference the 'name' field");
    }

    @Test
    void validate_errorMessageKeyMatchesExpected() {
        GroupEntity group = new GroupEntity(); // id is null

        RequireMandatoryFieldsRule<GroupEntity> rule = new RequireMandatoryFieldsRule<>(GroupEntity.class, registry);
        List<Message> errors = rule.validate(group);

        assertFalse(errors.isEmpty());
        assertEquals("common.errors.validation.mandatoryField", errors.get(0).getMessageKey(),
                "Message key must be the standard mandatory-field key");
    }

    // -------------------------------------------------------------------------
    // TaxClassEntity tests — countryRef is mandatory via @MetaMandatoryField
    // -------------------------------------------------------------------------

    @Test
    void validate_taxClassWithCountryRef_passes() {
        TaxClassEntity taxClass = new TaxClassEntity("TC-001");
        CountryEntity de = new CountryEntity();
        de.setIsoKey("DE");
        taxClass.setCountry(de);
        taxClass.setTaxRate(new java.math.BigDecimal("0.19"));

        RequireMandatoryFieldsRule<TaxClassEntity> rule = new RequireMandatoryFieldsRule<>(TaxClassEntity.class, registry);
        assertTrue(rule.validate(taxClass).isEmpty(), "TaxClass with countryRef must pass");
    }

    @Test
    void validate_taxClassWithoutCountryRef_returnsError() {
        TaxClassEntity taxClass = new TaxClassEntity("TC-002");
        // countryRef is null — must be rejected

        RequireMandatoryFieldsRule<TaxClassEntity> rule = new RequireMandatoryFieldsRule<>(TaxClassEntity.class, registry);
        List<Message> errors = rule.validate(taxClass);

        assertFalse(errors.isEmpty(), "Missing countryRef must produce a validation error");
        assertTrue(errors.stream().anyMatch(m -> m.getFields().contains("countryRef")),
                "Error must reference the 'countryRef' field");
    }

    // -------------------------------------------------------------------------
    // Unregistered entity — graceful fallback
    // -------------------------------------------------------------------------

    @Test
    void validate_entityNotInRegistry_returnsNoErrors() {
        EntityMetaInfoRegistry emptyRegistry = new EntityMetaInfoRegistry();
        RequireMandatoryFieldsRule<GroupEntity> rule = new RequireMandatoryFieldsRule<>(GroupEntity.class, emptyRegistry);
        assertTrue(rule.validate(new GroupEntity()).isEmpty(),
                "Entity not in registry must not throw and must return no errors");
    }
}
