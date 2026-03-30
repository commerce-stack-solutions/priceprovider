package de.ebusyness.priceproviderservice.service.language.validation;

import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.web.rest.Message;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Validation rule: A language cannot be inactive and mandatory at the same time.
 */
@Component
public class LanguageInactiveMandatoryRule implements ValidationRule<LanguageEntity> {
    
    @Override
    public List<Message> validate(LanguageEntity entity) {
        if (entity == null) {
            return Collections.emptyList();
        }
        
        if (Boolean.TRUE.equals(entity.getMandatory()) && !Boolean.TRUE.equals(entity.getActive())) {
            Message errorMessage = new Message(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_LANGUAGE_MANDATORY_MUST_BE_ACTIVE,
                Arrays.asList("active", "mandatory")
            );
            return Collections.singletonList(errorMessage);
        }
        return Collections.emptyList();
    }
}
