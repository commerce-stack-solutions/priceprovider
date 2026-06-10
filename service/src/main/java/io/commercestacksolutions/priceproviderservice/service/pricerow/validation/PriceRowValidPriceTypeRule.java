package io.commercestacksolutions.priceproviderservice.service.pricerow.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.type.PriceTypeRegistry;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PriceRowValidPriceTypeRule implements ValidationRule<PriceRowEntity> {

    private final PriceTypeRegistry registry;

    public PriceRowValidPriceTypeRule(PriceTypeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<Message> validate(PriceRowEntity entity) {
        if (entity == null || entity.getPriceType() == null) {
            return Collections.emptyList();
        }

        if (!registry.exists(entity.getPriceType().code())) {
            return Collections.singletonList(new Message(
                Message.MessageType.ERROR,
                "Unknown price type: " + entity.getPriceType().code(),
                List.of("priceType")
            ));
        }

        return Collections.emptyList();
    }
}
