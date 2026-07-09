package io.commercestacksolutions.priceproviderservice.service.channel.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeRegistry;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ChannelValidPriceRepresentationModeTypeRule implements ValidationRule<ChannelEntity> {

    private final PriceRepresentationModeRegistry registry;

    public ChannelValidPriceRepresentationModeTypeRule(PriceRepresentationModeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<Message> validate(ChannelEntity entity) {
        if (entity == null || entity.getPriceRepresentationMode() == null) {
            return Collections.emptyList();
        }

        if (!registry.exists(entity.getPriceRepresentationMode().code())) {
            return Collections.singletonList(new Message(
                Message.MessageType.ERROR,
                "Unknown price representation mode: " + entity.getPriceRepresentationMode().code(),
                List.of("priceRepresentationMode")
            ));
        }

        return Collections.emptyList();
    }
}
