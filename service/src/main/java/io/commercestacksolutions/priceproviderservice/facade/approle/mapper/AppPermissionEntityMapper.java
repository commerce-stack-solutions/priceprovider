package io.commercestacksolutions.priceproviderservice.facade.approle.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppPermissionRestEntity;
import org.springframework.stereotype.Component;

@Component
public class AppPermissionEntityMapper extends AbstractMapper<AppPermissionRestEntity, AppPermissionEntity, RestRequestMappingContext<String>> {

    @Override
    public AppPermissionEntity createTarget() {
        return new AppPermissionEntity();
    }

    @Override
    public void convert(AppPermissionRestEntity source, AppPermissionEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setId(context.getId());
        target.setDescription(source.getDescription());
    }
}
