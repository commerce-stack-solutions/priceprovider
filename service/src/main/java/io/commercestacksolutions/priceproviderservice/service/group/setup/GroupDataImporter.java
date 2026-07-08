package io.commercestacksolutions.priceproviderservice.service.group.setup;

import io.commercestacksolutions.commons.service.setup.AbstractSetupDataImporter;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.service.group.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GroupDataImporter extends AbstractSetupDataImporter<GroupEntity> {

    @Autowired
    public GroupDataImporter(GroupService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 70; // Load after currencies (60) but before units (100)
    }

    @Override
    public String getEntityTypeName() {
        return "Group";
    }

}
