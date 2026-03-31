package io.commercestacksolutions.priceproviderservice.facade.approle.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppPermissionRestEntity;
import org.springframework.stereotype.Component;

@Component
public class AppPermissionRestEntityMapper extends AbstractMapper<AppPermissionEntity, AppPermissionRestEntity, RestResponseMappingContext> {

    @Override
    public AppPermissionRestEntity createTarget() {
        return new AppPermissionRestEntity();
    }

    @Override
    public void convert(AppPermissionEntity source, AppPermissionRestEntity target, RestResponseMappingContext context) {
        target.setId(source.getId());
        target.setDescription(source.getDescription());

        if (context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(AppPermissionEntity source, AppPermissionRestEntity target, RestResponseMappingContext context) {
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
