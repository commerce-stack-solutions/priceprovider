package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype;

import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriceTypeRegistry extends EnumTypeValueRegistry<PriceType, PriceTypeDefinition> {

    public PriceTypeRegistry(List<PriceTypeDefinition> definitions) {
        super(definitions, PriceType::code);
    }

}
