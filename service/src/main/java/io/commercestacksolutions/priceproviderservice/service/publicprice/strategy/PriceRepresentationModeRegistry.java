package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriceRepresentationModeRegistry extends EnumTypeValueRegistry<PriceRepresentationModeType, PriceRepresentationMode> {

    public PriceRepresentationModeRegistry(List<PriceRepresentationMode> definitions) {
        super(definitions, PriceRepresentationModeType::code);
    }
}
