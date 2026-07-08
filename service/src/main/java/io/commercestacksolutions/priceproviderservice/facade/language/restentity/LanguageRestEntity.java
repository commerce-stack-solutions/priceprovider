package io.commercestacksolutions.priceproviderservice.facade.language.restentity;

import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;
import io.commercestacksolutions.commons.web.rest.RestEntity;

import java.util.Map;

public class LanguageRestEntity extends RestEntity<InfoAuditableRestEntity,IncludesLanguage> {
    private String isoKey;
    private Boolean active;
    private Boolean mandatory;
    private Map<String, String> name;

    public String getIsoKey() {
        return isoKey;
    }

    public void setIsoKey(String isoKey) {
        this.isoKey = isoKey;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }
}
