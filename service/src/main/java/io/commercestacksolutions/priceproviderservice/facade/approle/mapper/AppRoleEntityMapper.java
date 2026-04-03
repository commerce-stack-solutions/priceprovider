package io.commercestacksolutions.priceproviderservice.facade.approle.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AppRoleEntityMapper extends AbstractMapper<AppRoleRestEntity, AppRoleEntity, RestRequestMappingContext<Long>> {

    @Override
    public AppRoleEntity createTarget() {
        return new AppRoleEntity();
    }

    @Override
    public void convert(AppRoleRestEntity source, AppRoleEntity target, RestRequestMappingContext<Long> context) throws DataMappingException {
        if (source.getName() != null) {
            target.setName(source.getName());
        }
        target.setDescription(source.getDescription());

        // Convert permission name strings to stub AppPermissionEntity objects
        // The actual managed entities will be resolved in AppRoleServiceImpl.save()
        if (source.getPermissionRefs() != null) {
            Set<AppPermissionEntity> permissionRefs = new HashSet<>();
            for (String permissionName : source.getPermissionRefs()) {
                AppPermissionEntity permission = new AppPermissionEntity();
                permission.setName(permissionName);
                permissionRefs.add(permission);
            }
            target.setPermissionRefs(permissionRefs);
        } else {
            target.setPermissionRefs(new HashSet<>());
        }
    }
}
