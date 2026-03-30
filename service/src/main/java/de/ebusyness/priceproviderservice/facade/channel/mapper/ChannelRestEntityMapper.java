package de.ebusyness.priceproviderservice.facade.channel.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import de.ebusyness.priceproviderservice.dataaccess.country.entity.CountryEntity;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelRestEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class ChannelRestEntityMapper extends AbstractMapper<ChannelEntity, ChannelRestEntity, RestResponseMappingContext> {

    @Override
    public ChannelRestEntity createTarget() {
        return new ChannelRestEntity();
    }

    @Override
    public void convert(ChannelEntity source, ChannelRestEntity target, RestResponseMappingContext context) {
        target.setId(source.getId());

        // Map allowedCountryRefs to Set<String> of ISO keys
        if (source.getAllowedCountryRefs() != null) {
            Set<String> countryRefs = new HashSet<>();
            for (CountryEntity country : source.getAllowedCountryRefs()) {
                if (country != null && country.getIsoKey() != null) {
                    countryRefs.add(country.getIsoKey());
                }
            }
            target.setAllowedCountryRefs(countryRefs);
        } else {
            target.setAllowedCountryRefs(new HashSet<>());
        }

        target.setPriceRepresentationMode(source.getPriceRepresentationMode());

        if (context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(ChannelEntity source, ChannelRestEntity target, RestResponseMappingContext context) {
        InfoAuditableRestEntity info = new InfoAuditableRestEntity();
        if (context.expandWithAnyOf(new String[]{"$info", "$info.createdAt"})) {
            info.setCreatedAt(source.getCreatedAt());
        }
        if (context.expandWithAnyOf(new String[]{"$info", "$info.lastModifiedAt"})) {
            info.setLastModifiedAt(source.getLastModifiedAt());
        }
        target.setInfo(info);
    }
}
