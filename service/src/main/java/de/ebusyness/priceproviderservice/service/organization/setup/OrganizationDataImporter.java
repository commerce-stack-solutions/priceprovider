package de.ebusyness.priceproviderservice.service.organization.setup;

import de.ebusyness.commons.service.setup.AbstractSetupDataImporter;
import de.ebusyness.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import de.ebusyness.priceproviderservice.service.organization.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrganizationDataImporter extends AbstractSetupDataImporter<OrganizationEntity> {

    @Autowired
    public OrganizationDataImporter(OrganizationService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 75; // Load after groups (70) to ensure base groups exist first
    }

    @Override
    public String getEntityTypeName() {
        return "Organization";
    }

}
