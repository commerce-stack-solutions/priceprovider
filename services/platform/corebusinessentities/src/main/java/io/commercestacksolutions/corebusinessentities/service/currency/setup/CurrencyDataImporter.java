package io.commercestacksolutions.corebusinessentities.service.currency.setup;

import io.commercestacksolutions.corebusinessentities.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.commons.service.setup.AbstractSetupDataImporter;
import io.commercestacksolutions.corebusinessentities.service.currency.CurrencyService;
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
