package de.ebusyness.priceproviderservice.facade.country.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.priceproviderservice.dataaccess.country.entity.CountryEntity;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.facade.country.restentity.CountryRestEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class CountryRestEntityMapper extends AbstractMapper<CountryEntity, CountryRestEntity, RestResponseMappingContext> {

    @Override
    public CountryRestEntity createTarget() {
        return new CountryRestEntity();
    }

    @Override
    public void convert(CountryEntity source, CountryRestEntity target, RestResponseMappingContext context) {
        target.setIsoKey(source.getIsoKey());

        Map<String, String> nameMap = source.getName();
        if (nameMap == null) {
            target.setName(new HashMap<>());
        } else {
            target.setName(new HashMap<>(nameMap));
        }

        // Map allowedCurrencyRefs to Set<String> of currency keys
        if (source.getAllowedCurrencyRefs() != null) {
            Set<String> currencyRefs = new HashSet<>();
            for (CurrencyEntity currency : source.getAllowedCurrencyRefs()) {
                if (currency != null && currency.getCurrencyKey() != null) {
                    currencyRefs.add(currency.getCurrencyKey());
                }
            }
            target.setAllowedCurrencyRefs(currencyRefs);
        } else {
            target.setAllowedCurrencyRefs(new HashSet<>());
        }

        // Map primaryCurrencyRef to String currency key
        if (source.getPrimaryCurrencyRef() != null && source.getPrimaryCurrencyRef().getCurrencyKey() != null) {
            target.setPrimaryCurrencyRef(source.getPrimaryCurrencyRef().getCurrencyKey());
        } else {
            target.setPrimaryCurrencyRef(null);
        }

        if (context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(CountryEntity source, CountryRestEntity target, RestResponseMappingContext context) {
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
