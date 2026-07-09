package io.commercestacksolutions.priceproviderservice.facade.currency.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
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
