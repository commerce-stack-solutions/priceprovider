package de.ebusyness.priceproviderservice.facade.channel.restentity;

import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.commons.web.rest.RestEntity;

import java.util.Set;

public class ChannelRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesChannel> {

    private String id;
    private Set<String> allowedCountryRefs;
    private String priceRepresentationMode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getAllowedCountryRefs() {
        return allowedCountryRefs;
    }

    public void setAllowedCountryRefs(Set<String> allowedCountryRefs) {
        this.allowedCountryRefs = allowedCountryRefs;
    }

    public String getPriceRepresentationMode() {
        return priceRepresentationMode;
    }

    public void setPriceRepresentationMode(String priceRepresentationMode) {
        this.priceRepresentationMode = priceRepresentationMode;
    }
}
