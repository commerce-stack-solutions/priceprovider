package io.commercestacksolutions.priceproviderservice.service.pricerow.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Validation rule: checks for duplicate price rows (identical except id and priceValue)
 */
@Component
public class PriceRowDuplicateRule implements ValidationRule<PriceRowEntity> {

    private final PriceRowEntityRepository priceRowEntityRepository;

    @Autowired
    public PriceRowDuplicateRule(PriceRowEntityRepository priceRowEntityRepository) {
        this.priceRowEntityRepository = priceRowEntityRepository;
    }

    @Override
    public List<Message> validate(PriceRowEntity entity) {
        if (entity == null) {
            return Collections.emptyList();
        }

        List<PriceRowEntity> existingPriceRows = priceRowEntityRepository.findAll();
        for (PriceRowEntity existing : existingPriceRows) {
            // Skip if comparing with itself (same id)
            if (entity.getId() != null && entity.getId().equals(existing.getId())) {
                continue;
            }

            if (isDuplicate(entity, existing)) {
                Message errorMessage = new Message(
                    Message.MessageType.ERROR,
                    "A price row with identical fields (except id and priceValue) already exists with id: " + existing.getId(),
                    List.of("pricedResourceId", "minQuantity", "unitRef", "currency", "taxIncluded", "validFrom", "validTo", "groupRefs")
                );
                return Collections.singletonList(errorMessage);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Checks if two price rows are duplicates (all fields match except id and priceValue)
     */
    private boolean isDuplicate(PriceRowEntity pr1, PriceRowEntity pr2) {
        // Compare pricedResourceId
        if (!equals(pr1.getPricedResourceId(), pr2.getPricedResourceId())) {
            return false;
        }

        // Compare minQuantity
        if (!equals(pr1.getMinQuantity(), pr2.getMinQuantity())) {
            return false;
        }

        // Compare unit symbol
        String unit1 = pr1.getUnit() != null ? pr1.getUnit().getSymbol() : null;
        String unit2 = pr2.getUnit() != null ? pr2.getUnit().getSymbol() : null;
        if (!equals(unit1, unit2)) {
            return false;
        }

        // Compare currency
        if (!equals(pr1.getCurrency(), pr2.getCurrency())) {
            return false;
        }

        // Compare taxIncluded
        if (pr1.isTaxIncluded() != pr2.isTaxIncluded()) {
            return false;
        }

        // Compare validFrom
        if (!equals(pr1.getValidFrom(), pr2.getValidFrom())) {
            return false;
        }

        // Compare validTo
        if (!equals(pr1.getValidTo(), pr2.getValidTo())) {
            return false;
        }

        // Compare groups
        Set<String> groups1 = pr1.getGroups() != null ? 
            pr1.getGroups().stream().filter(g -> g != null && g.getPath() != null).map(g -> g.getPath()).collect(java.util.stream.Collectors.toSet()) : 
            new java.util.HashSet<>();
        Set<String> groups2 = pr2.getGroups() != null ? 
            pr2.getGroups().stream().filter(g -> g != null && g.getPath() != null).map(g -> g.getPath()).collect(java.util.stream.Collectors.toSet()) : 
            new java.util.HashSet<>();
        if (!equals(groups1, groups2)) {
            return false;
        }

        // All fields match (except id and priceValue which we intentionally skip)
        return true;
    }

    private boolean equals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }
}
