package io.commercestacksolutions.commons.web.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Metadata information for REST entities.
 * Provides structural information about entity fields and available enum values.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaInfo {
    
    private List<String> identityFields;
    private List<String> mandatoryFields;
    private List<String> referenceKeyFields;
    private Map<String, List<String>> enumValues;
    private List<FieldMetadata> fields;

    public MetaInfo() {
    }

    public MetaInfo(List<String> identityFields, List<String> mandatoryFields) {
        this.identityFields = identityFields;
        this.mandatoryFields = mandatoryFields;
    }

    public MetaInfo(List<String> identityFields, List<String> mandatoryFields, Map<String, List<String>> enumValues) {
        this.identityFields = identityFields;
        this.mandatoryFields = mandatoryFields;
        this.enumValues = enumValues;
    }

    public List<String> getIdentityFields() {
        return identityFields;
    }

    public void setIdentityFields(List<String> identityFields) {
        this.identityFields = identityFields;
    }

    public List<String> getMandatoryFields() {
        return mandatoryFields;
    }

    public void setMandatoryFields(List<String> mandatoryFields) {
        this.mandatoryFields = mandatoryFields;
    }

    public List<String> getReferenceKeyFields() {
        return referenceKeyFields;
    }

    public void setReferenceKeyFields(List<String> referenceKeyFields) {
        this.referenceKeyFields = referenceKeyFields;
    }

    public Map<String, List<String>> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(Map<String, List<String>> enumValues) {
        this.enumValues = enumValues;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public void setFields(List<FieldMetadata> fields) {
        this.fields = fields;
    }

    /**
     * Holds metadata for a single entity field to enable generic form UI generation.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldMetadata {
        private String name;
        private String type; // "Number", "Enum", "LocalizedString", "Reference", "Set<Reference>", "String", "DateTime", "Boolean"
        private Boolean readOnly;
        private Integer precision;
        private List<String> enumValues;

        public FieldMetadata() {
        }

        public FieldMetadata(String name, String type, Boolean readOnly) {
            this.name = name;
            this.type = type;
            this.readOnly = readOnly;
        }

        public FieldMetadata(String name, String type, Boolean readOnly, Integer precision, List<String> enumValues) {
            this.name = name;
            this.type = type;
            this.readOnly = readOnly;
            this.precision = precision;
            this.enumValues = enumValues;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getReadOnly() {
            return readOnly;
        }

        public void setReadOnly(Boolean readOnly) {
            this.readOnly = readOnly;
        }

        public Integer getPrecision() {
            return precision;
        }

        public void setPrecision(Integer precision) {
            this.precision = precision;
        }

        public List<String> getEnumValues() {
            return enumValues;
        }

        public void setEnumValues(List<String> enumValues) {
            this.enumValues = enumValues;
        }
    }
}
