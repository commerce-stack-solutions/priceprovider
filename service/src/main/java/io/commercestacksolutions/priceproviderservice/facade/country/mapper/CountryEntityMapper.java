package io.commercestacksolutions.priceproviderservice.facade.country.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.facade.country.restentity.CountryRestEntity;
import io.commercestacksolutions.priceproviderservice.service.currency.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class CountryEntityMapper extends AbstractMapper<CountryRestEntity, CountryEntity, RestRequestMappingContext<String>> {

    private final CurrencyService currencyService;

    @Autowired
    public CountryEntityMapper(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Override
    public CountryEntity createTarget() {
        return new CountryEntity();
    }

    @Override
    public void convert(CountryRestEntity source, CountryEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setIsoKey(context.getId());
        target.setName(source.getName() != null ? new HashMap<>(source.getName()) : new HashMap<>());

        // Map allowedCurrencyRefs: resolve each key to a managed CurrencyEntity
        if (source.getAllowedCurrencyRefs() != null && !source.getAllowedCurrencyRefs().isEmpty()) {
            Set<CurrencyEntity> resolvedCurrencies = new HashSet<>();
            for (String key : source.getAllowedCurrencyRefs()) {
                CurrencyEntity currency = currencyService.getCurrency(key);
                if (currency == null) {
                    Map<String, String> params = new HashMap<>();
                    params.put("entityType", "Currency");
                    params.put("idField", "currencyKey");
                    params.put("id", key);
                    throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
                }
                resolvedCurrencies.add(currency);
            }
            target.setAllowedCurrencyRefs(resolvedCurrencies);
        } else {
            target.setAllowedCurrencyRefs(new HashSet<>());
        }

        // Map primaryCurrencyRef: resolve key to a managed CurrencyEntity
        if (source.getPrimaryCurrencyRef() != null && !source.getPrimaryCurrencyRef().isBlank()) {
            String key = source.getPrimaryCurrencyRef();
            CurrencyEntity currency = currencyService.getCurrency(key);
            if (currency == null) {
                Map<String, String> params = new HashMap<>();
                params.put("entityType", "Currency");
                params.put("idField", "currencyKey");
                params.put("id", key);
                throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
            }
            target.setPrimaryCurrencyRef(currency);
        } else {
            target.setPrimaryCurrencyRef(null);
        }
    }
}
