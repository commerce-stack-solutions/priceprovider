package io.commercestacksolutions.priceproviderservice.facade.approle.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.info.InfoAppRole;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AppRoleRestEntityMapper extends AbstractMapper<AppRoleEntity, AppRoleRestEntity, RestResponseMappingContext> {

    @Override
    public AppRoleRestEntity createTarget() {
        return new AppRoleRestEntity();
    }

    @Override
    public void convert(AppRoleEntity source, AppRoleRestEntity target, RestResponseMappingContext context) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());

        // Convert permission entities to name strings
        if (source.getPermissionRefs() != null) {
            Set<String> permissionRefs = source.getPermissionRefs().stream()
                    .filter(p -> p != null && p.getName() != null)
                    .map(AppPermissionEntity::getName)
                    .collect(Collectors.toSet());
            target.setPermissionRefs(permissionRefs);
        }

        // Always populate $info with permissionRefIds for navigation links
        addInfo(source, target, context);
    }

    private void addInfo(AppRoleEntity source, AppRoleRestEntity target, RestResponseMappingContext context) {
        InfoAppRole info = new InfoAppRole();

        // Always populate permissionRefIds in $info for UI navigation links (name → id map)
        if (source.getPermissionRefs() != null) {
            Map<String, Long> permissionRefIds = source.getPermissionRefs().stream()
                    .filter(p -> p != null && p.getName() != null && p.getId() != null)
                    .collect(Collectors.toMap(AppPermissionEntity::getName, AppPermissionEntity::getId));
            if (!permissionRefIds.isEmpty()) {
                info.setPermissionRefIds(permissionRefIds);
            }
        }

        // Add audit timestamps to $info when requested
        if (context.expandWithAnyOf(new String[]{"$info", "$info.createdAt"})) {
            info.setCreatedAt(source.getCreatedAt());
        }
        if (context.expandWithAnyOf(new String[]{"$info", "$info.lastModifiedAt"})) {
            info.setLastModifiedAt(source.getLastModifiedAt());
        }

        target.setInfo(info);
    }
}
