package de.ebusyness.priceproviderservice.service.currency.validation;

import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.language.LanguageEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validation rule that ensures Currency.name contains values for all mandatory languages.
 * This ensures that currency names are available in all languages marked as mandatory.
 */
@Component
public class CurrencyLocalizedNameRule implements ValidationRule<CurrencyEntity> {

    private final LanguageEntityRepository languageRepository;

    @Autowired
    public CurrencyLocalizedNameRule(LanguageEntityRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Override
    public List<Message> validate(CurrencyEntity entity) {
        List<Message> errors = new ArrayList<>();

        if (entity == null || entity.getName() == null) {
            return errors;
        }

        // Get mandatory languages from database
        Set<String> mandatoryLanguageCodes = languageRepository.findByMandatory(true).stream()
                .map(LanguageEntity::getIsoKey)
                .collect(Collectors.toSet());

        // Check if all mandatory languages have values in the name field
        Map<String, String> nameMap = entity.getName();
        List<String> missingLanguages = new ArrayList<>();

        for (String languageCode : mandatoryLanguageCodes) {
            String value = nameMap.get(languageCode);
            if (value == null || value.trim().isEmpty()) {
                missingLanguages.add(languageCode);
            }
        }

        if (!missingLanguages.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("field", "name");
            params.put("languages", String.join(", ", missingLanguages));

            errors.add(new Message(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE,
                    params,
                    List.of("name")
            ));
        }

        return errors;
    }
}
