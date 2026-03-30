package de.ebusyness.priceproviderservice.facade.language.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import de.ebusyness.priceproviderservice.facade.language.restentity.LanguageRestEntity;
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
