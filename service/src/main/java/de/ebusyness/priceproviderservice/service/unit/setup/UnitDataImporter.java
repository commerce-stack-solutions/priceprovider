package de.ebusyness.priceproviderservice.service.unit.setup;

import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import de.ebusyness.commons.service.setup.AbstractSetupDataImporter;
import de.ebusyness.priceproviderservice.service.unit.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UnitDataImporter extends AbstractSetupDataImporter<UnitEntity> {

    @Autowired
    public UnitDataImporter(UnitService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String getEntityTypeName() {
        return "Unit";
    }

}