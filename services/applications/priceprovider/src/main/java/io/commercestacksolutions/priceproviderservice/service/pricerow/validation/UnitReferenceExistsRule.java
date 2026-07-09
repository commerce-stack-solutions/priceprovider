package io.commercestacksolutions.priceproviderservice.service.pricerow.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.unit.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Validation rule: unit reference must exist
 */
@Component
public class UnitReferenceExistsRule implements ValidationRule<PriceRowEntity> {

    private final UnitService unitEntityService;

    @Autowired
    public UnitReferenceExistsRule(UnitService unitEntityService) {
        this.unitEntityService = unitEntityService;
    }

    @Override
    public List<Message> validate(PriceRowEntity entity) {
        if (entity == null) {
            return Collections.emptyList();
        }

        if (entity.getUnit() != null && entity.getUnit().getSymbol() != null) {
            if (unitEntityService.getUnit(entity.getUnit().getSymbol()) == null) {
                Message errorMessage = new Message(
                    Message.MessageType.ERROR,
                    "Invalid unit reference: " + entity.getUnit().getSymbol() + " does not exist",
                    List.of("unitRef")
                );
                return Collections.singletonList(errorMessage);
            }
        }

        return Collections.emptyList();
    }
}
