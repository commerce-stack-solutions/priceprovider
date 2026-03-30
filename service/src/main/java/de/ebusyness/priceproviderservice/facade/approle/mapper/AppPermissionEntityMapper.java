package de.ebusyness.priceproviderservice.facade.approle.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppPermissionRestEntity;
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
