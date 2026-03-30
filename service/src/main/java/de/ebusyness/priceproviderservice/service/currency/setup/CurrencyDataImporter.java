package de.ebusyness.priceproviderservice.service.currency.setup;

import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.commons.service.setup.AbstractSetupDataImporter;
import de.ebusyness.priceproviderservice.service.currency.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CurrencyDataImporter extends AbstractSetupDataImporter<CurrencyEntity> {

    @Autowired
    public CurrencyDataImporter(CurrencyService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 60; // Load after languages (50) but before units (100) and price rows
    }

    @Override
    public String getEntityTypeName() {
        return "Currency";
    }

}
