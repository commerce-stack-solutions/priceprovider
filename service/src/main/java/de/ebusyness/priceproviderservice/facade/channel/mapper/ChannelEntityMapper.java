package de.ebusyness.priceproviderservice.facade.channel.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import de.ebusyness.priceproviderservice.dataaccess.country.entity.CountryEntity;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelRestEntity;
import de.ebusyness.priceproviderservice.service.country.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ChannelEntityMapper extends AbstractMapper<ChannelRestEntity, ChannelEntity, RestRequestMappingContext<String>> {

    private final CountryService countryService;

    @Autowired
    public ChannelEntityMapper(CountryService countryService) {
        this.countryService = countryService;
    }

    @Override
    public ChannelEntity createTarget() {
        return new ChannelEntity();
    }

    @Override
    public void convert(ChannelRestEntity source, ChannelEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setId(context.getId());

        // Handle countryRefs - resolve each country ref to a CountryEntity
        if (source.getAllowedCountryRefs() != null && !source.getAllowedCountryRefs().isEmpty()) {
            Set<CountryEntity> allowedCountries = new HashSet<>();
            for (String countryRef : source.getAllowedCountryRefs()) {
                CountryEntity country = countryService.getCountry(countryRef);
                if (country == null) {
                    Map<String, String> params = new HashMap<>();
                    params.put("entityType", "Country");
                    params.put("idField", "isoKey");
                    params.put("id", countryRef);
                    throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
                }
                allowedCountries.add(country);
            }
            target.setAllowedCountryRefs(allowedCountries);
        } else {
            target.setAllowedCountryRefs(new HashSet<>());
        }

        target.setPriceRepresentationMode(source.getPriceRepresentationMode());
    }
}
