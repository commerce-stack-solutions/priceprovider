package io.commercestacksolutions.priceproviderservice.service.pricerow.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Validation rule that ensures the 'priceValue' field of a {@link PriceRowEntity}
 * does not exceed the precision limit of 2 decimal places.
 */
@Component
public class PriceRowPrecisionValidationRule implements ValidationRule<PriceRowEntity> {

    @Override
    public List<Message> validate(PriceRowEntity entity) {
        if (entity == null || entity.getPriceValue() == null) {
            return Collections.emptyList();
        }

        BigDecimal priceValue = entity.getPriceValue();
        // Strip trailing zeros to get the actual mathematical scale
        BigDecimal stripped = priceValue.stripTrailingZeros();

        if (stripped.scale() > 2) {
            return Collections.singletonList(new Message(
                    Message.MessageType.ERROR,
                    "Field 'priceValue' exceeds allowed precision of 2 decimal places",
                    List.of("priceValue")
            ));
        }

        return Collections.emptyList();
    }
}
