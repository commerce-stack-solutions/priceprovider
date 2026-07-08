package io.commercestacksolutions.priceproviderservice.service.pricerow.setup;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.pricerow.PriceRowService;
import io.commercestacksolutions.commons.service.setup.AbstractSetupDataImporter;
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