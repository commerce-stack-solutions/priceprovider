package de.ebusyness.priceproviderservice.service.language.setup;

import de.ebusyness.commons.service.setup.AbstractSetupDataImporter;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import de.ebusyness.priceproviderservice.service.language.LanguageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LanguageDataImporter extends AbstractSetupDataImporter<LanguageEntity> {

    @Autowired
    public LanguageDataImporter(LanguageService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 50; // Load before units (priority 100) as languages may be needed for other entities
    }

    @Override
    public String getEntityTypeName() {
        return "Language";
    }

}
