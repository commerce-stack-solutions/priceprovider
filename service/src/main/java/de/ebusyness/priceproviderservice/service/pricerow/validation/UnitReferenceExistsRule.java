package de.ebusyness.priceproviderservice.service.pricerow.validation;

import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.service.unit.UnitService;
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
