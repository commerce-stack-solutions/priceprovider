package de.ebusyness.priceproviderservice.facade.approle.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;
import org.springframework.stereotype.Component;

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
        target.setDescription(source.getDescription());

        // Convert permission entities to ID strings
        if (source.getPermissionRefs() != null) {
            Set<String> permissionRefs = source.getPermissionRefs().stream()
                    .filter(p -> p != null && p.getId() != null)
                    .map(AppPermissionEntity::getId)
                    .collect(Collectors.toSet());
            target.setPermissionRefs(permissionRefs);
        }

        if (context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(AppRoleEntity source, AppRoleRestEntity target, RestResponseMappingContext context) {
        InfoAuditableRestEntity info = new InfoAuditableRestEntity();
        if (context.expandWithAnyOf(new String[]{"$info", "$info.createdAt"})) {
            info.setCreatedAt(source.getCreatedAt());
        }
        if (context.expandWithAnyOf(new String[]{"$info", "$info.lastModifiedAt"})) {
            info.setLastModifiedAt(source.getLastModifiedAt());
        }
        target.setInfo(info);
    }
}
