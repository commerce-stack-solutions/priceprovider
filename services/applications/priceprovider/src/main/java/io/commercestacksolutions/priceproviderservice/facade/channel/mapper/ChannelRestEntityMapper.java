package io.commercestacksolutions.priceproviderservice.facade.channel.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.facade.channel.restentity.ChannelRestEntity;
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

        target.setPriceRepresentationMode(source.getPriceRepresentationMode() != null ? source.getPriceRepresentationMode().code() : null);

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
