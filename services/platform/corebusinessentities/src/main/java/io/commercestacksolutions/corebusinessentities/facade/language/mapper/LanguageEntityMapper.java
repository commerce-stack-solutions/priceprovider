package io.commercestacksolutions.corebusinessentities.facade.language.mapper;

import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.corebusinessentities.facade.language.restentity.LanguageRestEntity;
import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import org.springframework.stereotype.Component;

@Component
public class LanguageEntityMapper extends AbstractMapper<LanguageRestEntity, LanguageEntity, RestRequestMappingContext<String>> {

    @Override
    public LanguageEntity createTarget() {
        return new LanguageEntity();
    }

    @Override
    public void convert(LanguageRestEntity source, LanguageEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setIsoKey(context.getId());
        target.setActive(source.getActive());
        target.setMandatory(source.getMandatory());
        target.setName(source.getName());
    }
}
