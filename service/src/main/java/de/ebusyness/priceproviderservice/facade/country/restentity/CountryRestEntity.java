package de.ebusyness.priceproviderservice.facade.country.restentity;

import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.commons.web.rest.RestEntity;

import java.util.Map;
import java.util.Set;

public class CountryRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesCountry> {

    private String isoKey;
    private Map<String, String> name;
    private Set<String> allowedCurrencyRefs;
    private String primaryCurrencyRef;

    public String getIsoKey() {
        return isoKey;
    }

    public void setIsoKey(String isoKey) {
        this.isoKey = isoKey;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }

    public Set<String> getAllowedCurrencyRefs() {
        return allowedCurrencyRefs;
    }

    public void setAllowedCurrencyRefs(Set<String> allowedCurrencyRefs) {
        this.allowedCurrencyRefs = allowedCurrencyRefs;
    }

    public String getPrimaryCurrencyRef() {
        return primaryCurrencyRef;
    }

    public void setPrimaryCurrencyRef(String primaryCurrencyRef) {
        this.primaryCurrencyRef = primaryCurrencyRef;
    }
}
