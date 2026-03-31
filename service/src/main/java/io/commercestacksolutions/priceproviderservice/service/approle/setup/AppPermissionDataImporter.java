package io.commercestacksolutions.priceproviderservice.service.approle.setup;

import io.commercestacksolutions.commons.service.setup.AbstractSetupDataImporter;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.service.approle.AppPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppPermissionDataImporter extends AbstractSetupDataImporter<AppPermissionEntity> {

    @Autowired
    public AppPermissionDataImporter(AppPermissionService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 45; // Load before AppRole (46)
    }

    @Override
    public String getEntityTypeName() {
        return "AppPermission";
    }
}
