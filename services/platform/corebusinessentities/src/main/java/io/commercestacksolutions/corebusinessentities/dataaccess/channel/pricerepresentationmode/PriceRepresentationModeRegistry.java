package io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode;

import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriceRepresentationModeRegistry extends EnumTypeValueRegistry<PriceRepresentationModeType, PriceRepresentationModeDefinition> {

    public PriceRepresentationModeRegistry(List<PriceRepresentationModeDefinition> definitions) {
        super(definitions, PriceRepresentationModeType::code);
    }
}
