package io.commercestacksolutions.corebusinessentities.service.language.validation;

import io.commercestacksolutions.corebusinessentities.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.LanguageEntityRepository;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LanguageLocalizedNameRule implements ValidationRule<LanguageEntity> {

    private final LanguageEntityRepository languageRepository;

    @Autowired
    public LanguageLocalizedNameRule(LanguageEntityRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Override
    public List<Message> validate(LanguageEntity entity) {
        List<Message> errors = new ArrayList<>();

        if (entity == null || entity.getName() == null) {
            return errors;
        }

        Set<String> mandatoryLanguageCodes = languageRepository.findByMandatory(true).stream()
                .map(LanguageEntity::getIsoKey)
                .collect(Collectors.toSet());

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
