package de.ebusyness.priceproviderservice.facade.currency.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CurrencyRestEntityMapper extends AbstractMapper<CurrencyEntity, CurrencyRestEntity, RestResponseMappingContext> {

    @Override
    public CurrencyRestEntity createTarget() {
        return new CurrencyRestEntity();
    }

    @Override
    public void convert(CurrencyEntity source, CurrencyRestEntity target, RestResponseMappingContext context) {
        target.setCurrencyKey(source.getCurrencyKey());
        target.setSymbol(source.getSymbol());
        Map<String, String> names = source.getName();
        if (names != null) {
            target.setName(new HashMap<>(names));
        } else {
            target.setName(null);
        }

        if(context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(CurrencyEntity source, CurrencyRestEntity target, RestResponseMappingContext context) {
        // Add audit timestamps to $info
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
