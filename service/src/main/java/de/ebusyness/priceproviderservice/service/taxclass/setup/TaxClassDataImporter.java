package de.ebusyness.priceproviderservice.service.taxclass.setup;

import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.commons.service.setup.AbstractSetupDataImporter;
import de.ebusyness.priceproviderservice.service.taxclass.TaxClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaxClassDataImporter extends AbstractSetupDataImporter<TaxClassEntity> {

    @Autowired
    public TaxClassDataImporter(TaxClassService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 70; // Load after currencies (60) but before price rows
    }

    @Override
    public String getEntityTypeName() {
        return "TaxClass";
    }

}
