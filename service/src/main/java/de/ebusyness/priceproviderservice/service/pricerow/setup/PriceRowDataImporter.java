package de.ebusyness.priceproviderservice.service.pricerow.setup;

import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.service.pricerow.PriceRowService;
import de.ebusyness.commons.service.setup.AbstractSetupDataImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PriceRowDataImporter extends AbstractSetupDataImporter<PriceRowEntity> {

    @Autowired
    public PriceRowDataImporter(PriceRowService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 200;
    }

    @Override
    public String getEntityTypeName() {
        return "PriceRow";
    }

}