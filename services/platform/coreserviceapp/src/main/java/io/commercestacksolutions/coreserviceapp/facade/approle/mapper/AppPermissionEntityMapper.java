package io.commercestacksolutions.coreserviceapp.facade.approle.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.coreserviceapp.dataaccess.approle.entity.CommonAppPermission;
import io.commercestacksolutions.coreserviceapp.facade.approle.restentity.AppPermissionRestEntity;
import org.springframework.stereotype.Component;

@Component
public class AppPermissionEntityMapper extends AbstractMapper<AppPermissionRestEntity, CommonAppPermission, RestRequestMappingContext<Long>> {

    @Override
    public CommonAppPermission createTarget() {
        return new CommonAppPermission();
    }

    @Override
    public void convert(AppPermissionRestEntity source, CommonAppPermission target, RestRequestMappingContext<Long> context) throws DataMappingException {
        if (source.getName() != null) {
            target.setName(source.getName());
        }
        target.setDescription(source.getDescription());
    }
}
