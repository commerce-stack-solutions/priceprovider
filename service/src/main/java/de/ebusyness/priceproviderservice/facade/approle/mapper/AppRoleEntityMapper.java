package de.ebusyness.priceproviderservice.facade.approle.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AppRoleEntityMapper extends AbstractMapper<AppRoleRestEntity, AppRoleEntity, RestRequestMappingContext<String>> {

    @Override
    public AppRoleEntity createTarget() {
        return new AppRoleEntity();
    }

    @Override
    public void convert(AppRoleRestEntity source, AppRoleEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setId(context.getId());
        target.setDescription(source.getDescription());

        // Convert permission ID strings to stub AppPermissionEntity objects
        // The actual managed entities will be resolved in AppRoleServiceImpl.save()
        if (source.getPermissionRefs() != null) {
            Set<AppPermissionEntity> permissionRefs = new HashSet<>();
            for (String permissionId : source.getPermissionRefs()) {
                AppPermissionEntity permission = new AppPermissionEntity();
                permission.setId(permissionId);
                permissionRefs.add(permission);
            }
            target.setPermissionRefs(permissionRefs);
        } else {
            target.setPermissionRefs(new HashSet<>());
        }
    }
}
