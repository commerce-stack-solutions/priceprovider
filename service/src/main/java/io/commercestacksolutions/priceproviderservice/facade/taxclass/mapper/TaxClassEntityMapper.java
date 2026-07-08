package io.commercestacksolutions.priceproviderservice.facade.taxclass.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
import io.commercestacksolutions.priceproviderservice.service.country.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TaxClassEntityMapper extends AbstractMapper<TaxClassRestEntity, TaxClassEntity, RestRequestMappingContext<String>> {

    private final CountryService countryService;

    @Autowired
    public TaxClassEntityMapper(CountryService countryService) {
        this.countryService = countryService;
    }

    @Override
    public TaxClassEntity createTarget() {
        return new TaxClassEntity();
    }

    @Override
    public void convert(TaxClassRestEntity source, TaxClassEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setTaxClassId(context.getId());
        target.setTaxRate(source.getTaxRate());

        // Handle countryRef - resolve to CountryEntity if provided
        if (source.getCountryRef() != null && !source.getCountryRef().isEmpty()) {
            CountryEntity country = countryService.getCountry(source.getCountryRef());
            if (country == null) {
                Map<String, String> params = new HashMap<>();
                params.put("entityType", "Country");
                params.put("idField", "isoKey");
                params.put("id", source.getCountryRef());
                throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
            }
            target.setCountry(country);
        } else {
            target.setCountry(null);
        }
    }
}
