package de.ebusyness.priceproviderservice.facade.currency.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
import org.springframework.stereotype.Component;

@Component
public class CurrencyEntityMapper extends AbstractMapper<CurrencyRestEntity, CurrencyEntity, RestRequestMappingContext<String>> {

    @Override
    public CurrencyEntity createTarget() {
        return new CurrencyEntity();
    }

    @Override
    public void convert(CurrencyRestEntity source, CurrencyEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setCurrencyKey(context.getId());
        target.setSymbol(source.getSymbol());
        target.setName(source.getName());
    }
}
