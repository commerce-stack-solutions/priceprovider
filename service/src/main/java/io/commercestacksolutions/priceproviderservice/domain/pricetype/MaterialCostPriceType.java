package io.commercestacksolutions.priceproviderservice.domain.pricetype;

import org.springframework.stereotype.Component;

@Component
public class MaterialCostPriceType implements PriceTypeDefinition {
    @Override
    public PriceType getPriceType() {
        return new PriceType("MATERIAL_COST");
    }

    @Override
    public String getDisplayName() {
        return "Material Cost";
    }
}
