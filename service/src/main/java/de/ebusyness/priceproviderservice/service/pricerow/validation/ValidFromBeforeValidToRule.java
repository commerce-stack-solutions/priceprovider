package de.ebusyness.priceproviderservice.service.pricerow.validation;

import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Validation rule: validFrom must be before validTo
 */
@Component
public class ValidFromBeforeValidToRule implements ValidationRule<PriceRowEntity> {

    @Override
    public List<Message> validate(PriceRowEntity entity) {
        if (entity == null) {
            return Collections.emptyList();
        }

        if (entity.getValidFrom() != null && entity.getValidTo() != null) {
            if (entity.getValidFrom().isAfter(entity.getValidTo())) {
                Message errorMessage = new Message(
                    Message.MessageType.ERROR,
                    "validFrom must be before validTo",
                    List.of("validFrom", "validTo")
                );
                return Collections.singletonList(errorMessage);
            }
        }

        return Collections.emptyList();
    }
}
