package de.ebusyness.commons.web.rest;

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
    private Map<String, List<String>> enumValues;

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

    public Map<String, List<String>> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(Map<String, List<String>> enumValues) {
        this.enumValues = enumValues;
    }
}
