package io.commercestacksolutions.corebusinessentities.service.language.setup;

import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.corebusinessentities.service.language.LanguageService;
import io.commercestacksolutions.commons.service.setup.AbstractSetupDataImporter;
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
        return 50;
    }

    @Override
    public String getEntityTypeName() {
        return "Language";
    }
}
