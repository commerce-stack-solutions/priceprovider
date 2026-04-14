package io.commercestacksolutions.commons.dataaccess.meta;

import io.commercestacksolutions.commons.dataaccess.ReferenceKey;
import io.commercestacksolutions.commons.dataaccess.idgenerator.GeneratedId;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MetaInfoBuilder}.
 */
public class MetaInfoBuilderTest {

    // ---------- test fixtures ----------

    enum Color { RED, GREEN, BLUE }

    static class BaseEntity {
        @Id
        private String id;          // no @GeneratedValue → auto-mandatory
        @MandatoryField
        private String name;
    }

    static class ChildEntity extends BaseEntity {
        @MandatoryField
        private Color color;        // mandatory enum
        private Color optionalColor; // optional enum – values must still be included
        private String description;  // not mandatory
    }

    static class GeneratedIdEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;            // @GeneratedValue → identity field but NOT auto-mandatory
        @MandatoryField
        private String name;
    }

    static class GeneratedIdAnnotationEntity {
        @Id
        @GeneratedId
        private String id;          // @GeneratedId → auto-generated UUID, NOT mandatory
        @MandatoryField
        private String name;
    }

    static class EntityWithReferenceKey {
        @Id
        @GeneratedId
        private String id;
        @ReferenceKey
        private String path;
        @MandatoryField
        private String name;
    }

    // ---------- tests ----------

    @Test
    void build_detectsIdAnnotationAsIdentityField() {
        MetaInfo meta = MetaInfoBuilder.build(BaseEntity.class);
        assertNotNull(meta.getIdentityFields());
        assertTrue(meta.getIdentityFields().contains("id"),
                "Field annotated with @Id must appear in identityFields");
    }

    @Test
    void build_idWithoutGeneratedValueIsAutoMandatory() {
        MetaInfo meta = MetaInfoBuilder.build(BaseEntity.class);
        assertNotNull(meta.getMandatoryFields());
        assertTrue(meta.getMandatoryFields().contains("id"),
                "@Id field without @GeneratedValue must be auto-included in mandatoryFields");
    }

    @Test
    void build_idWithGeneratedValueIsNotMandatory() {
        MetaInfo meta = MetaInfoBuilder.build(GeneratedIdEntity.class);
        assertTrue(meta.getIdentityFields().contains("id"),
                "@Id @GeneratedValue field must still appear in identityFields");
        assertFalse(meta.getMandatoryFields().contains("id"),
                "@Id field with @GeneratedValue must NOT appear in mandatoryFields (DB assigns the value)");
    }

    @Test
    void build_idWithGeneratedIdAnnotationIsNotMandatory() {
        MetaInfo meta = MetaInfoBuilder.build(GeneratedIdAnnotationEntity.class);
        assertTrue(meta.getIdentityFields().contains("id"),
                "@Id @GeneratedId field must still appear in identityFields");
        assertFalse(meta.getMandatoryFields().contains("id"),
                "@Id field with @GeneratedId must NOT appear in mandatoryFields (auto-generated via IdGenerator)");
        assertTrue(meta.getMandatoryFields().contains("name"),
                "@MandatoryField 'name' must still be mandatory");
    }

    @Test
    void build_collectsMandatoryFieldsFromAnnotation() {
        MetaInfo meta = MetaInfoBuilder.build(BaseEntity.class);
        assertNotNull(meta.getMandatoryFields());
        assertTrue(meta.getMandatoryFields().contains("name"),
                "Field annotated with @MandatoryField must appear in mandatoryFields");
    }

    @Test
    void build_traversesClassHierarchy() {
        MetaInfo meta = MetaInfoBuilder.build(ChildEntity.class);
        List<String> mandatory = meta.getMandatoryFields();
        assertNotNull(mandatory);
        assertTrue(mandatory.contains("id"),    "Auto-mandatory @Id from superclass must be included");
        assertTrue(mandatory.contains("name"),  "Inherited mandatory field 'name' must be included");
        assertTrue(mandatory.contains("color"), "Child mandatory field 'color' must be included");
    }

    @Test
    void build_includesIdentityFieldFromSuperclass() {
        MetaInfo meta = MetaInfoBuilder.build(ChildEntity.class);
        assertTrue(meta.getIdentityFields().contains("id"),
                "Identity field from superclass must be discovered");
    }

    @Test
    void build_includesEnumValuesForMandatoryEnumField() {
        MetaInfo meta = MetaInfoBuilder.build(ChildEntity.class);
        assertNotNull(meta.getEnumValues(), "enumValues must not be null when enum fields are present");
        assertTrue(meta.getEnumValues().containsKey("color"),
                "Mandatory enum field must have its values in enumValues");
        List<String> colorValues = meta.getEnumValues().get("color");
        assertTrue(colorValues.containsAll(List.of("RED", "GREEN", "BLUE")),
                "All enum constants must be listed");
    }

    @Test
    void build_includesEnumValuesForOptionalEnumField() {
        MetaInfo meta = MetaInfoBuilder.build(ChildEntity.class);
        assertNotNull(meta.getEnumValues());
        assertTrue(meta.getEnumValues().containsKey("optionalColor"),
                "Optional (non-mandatory) enum field must also have its values included in enumValues");
    }

    @Test
    void build_nonEnumNonMandatoryFieldNotInMandatoryList() {
        MetaInfo meta = MetaInfoBuilder.build(ChildEntity.class);
        assertFalse(meta.getMandatoryFields().contains("description"),
                "Field without @MandatoryField must not appear in mandatoryFields");
    }

    @Test
    void build_idNotDuplicatedInMandatoryFieldsWhenBothAnnotationsPresent() {
        // If someone annotates @Id @MandatoryField (redundant but harmless), id must appear only once
        @SuppressWarnings("unused")
        class RedundantEntity {
            @Id
            @MandatoryField   // redundant – @Id already implies mandatory
            private String id;
        }
        MetaInfo meta = MetaInfoBuilder.build(RedundantEntity.class);
        long count = meta.getMandatoryFields().stream().filter("id"::equals).count();
        assertEquals(1, count, "id must appear exactly once in mandatoryFields even with redundant annotations");
    }

    @Test
    void build_forGroupEntity_containsExpectedFields() {
        MetaInfo meta = MetaInfoBuilder.build(
                io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity.class);

        assertTrue(meta.getIdentityFields().contains("id"),    "GroupEntity: id must be identityField");
        assertFalse(meta.getMandatoryFields().contains("id"),  "GroupEntity: id must NOT be mandatory (auto-generated UUID)");
        assertTrue(meta.getMandatoryFields().contains("path"), "GroupEntity: path must be mandatory");
        assertTrue(meta.getMandatoryFields().contains("name"), "GroupEntity: name must be mandatory");
        assertNotNull(meta.getReferenceKeyFields(),             "GroupEntity: referenceKeyFields must not be null");
        assertTrue(meta.getReferenceKeyFields().contains("path"), "GroupEntity: path must be in referenceKeyFields (@ReferenceKey)");
        assertFalse(meta.getReferenceKeyFields().contains("id"),  "GroupEntity: id must NOT be in referenceKeyFields");
    }

    @Test
    void build_withReferenceKey_populatesReferenceKeyFields() {
        MetaInfo meta = MetaInfoBuilder.build(EntityWithReferenceKey.class);
        assertNotNull(meta.getReferenceKeyFields());
        assertTrue(meta.getReferenceKeyFields().contains("path"),
                "@ReferenceKey field must appear in referenceKeyFields");
        assertFalse(meta.getReferenceKeyFields().contains("id"),
                "Technical @Id must not appear in referenceKeyFields when @ReferenceKey is present");
    }

    @Test
    void build_withoutReferenceKey_fallsBackToIdentityFields() {
        MetaInfo meta = MetaInfoBuilder.build(BaseEntity.class);
        assertNotNull(meta.getReferenceKeyFields(),
                "referenceKeyFields must fall back to identityFields when no @ReferenceKey present");
        assertTrue(meta.getReferenceKeyFields().contains("id"),
                "Fallback referenceKeyFields must include the @Id field");
    }

    @Test
    void build_forPriceRowEntity_idIsIdentityButNotMandatory() {
        MetaInfo meta = MetaInfoBuilder.build(
                io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity.class);

        assertTrue(meta.getIdentityFields().contains("id"),
                "PriceRowEntity: id must be identityField");
        assertFalse(meta.getMandatoryFields().contains("id"),
                "PriceRowEntity: id must NOT be mandatory because it is @GeneratedValue");
        // Other mandatory fields must still be present
        assertTrue(meta.getMandatoryFields().contains("pricedResourceId"),
                "PriceRowEntity: pricedResourceId must be mandatory");
        assertTrue(meta.getMandatoryFields().contains("priceValue"),
                "PriceRowEntity: priceValue must be mandatory");
    }
}

