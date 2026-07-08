package io.commercestacksolutions.priceproviderservice.service.organization.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys.ERROR_VALIDATION_INVALID_REFERENCE;

/**
 * Validation rule: parent and sub references must exist in the database
 */
@Component
public class OrganizationReferenceExistsRule implements ValidationRule<OrganizationEntity> {

    private final GroupEntityRepository groupEntityRepository;

    @Autowired
    public OrganizationReferenceExistsRule(GroupEntityRepository groupEntityRepository) {
        this.groupEntityRepository = groupEntityRepository;
    }

    @Override
    public List<Message> validate(OrganizationEntity entity) {
        List<Message> errors = new ArrayList<>();
        
        if (entity == null) {
            return errors;
        }

        // Validate parent references
        if (entity.getParentRefs() != null && !entity.getParentRefs().isEmpty()) {
            for (GroupEntity parent : entity.getParentRefs()) {
                if (parent != null && parent.getPath() != null) {
                    if (!groupEntityRepository.existsByPath(parent.getPath())) {
                        Map<String, String> params = new HashMap<>();
                        params.put("referenceId", parent.getPath());
                        params.put("referenceType", "parent");
                        
                        errors.add(new Message(
                            Message.MessageType.ERROR,
                                ERROR_VALIDATION_INVALID_REFERENCE,
                            params,
                            List.of("parentRefs")
                        ));
                    }
                }
            }
        }

        // Validate sub references
        if (entity.getSubRefs() != null && !entity.getSubRefs().isEmpty()) {
            for (GroupEntity sub : entity.getSubRefs()) {
                if (sub != null && sub.getPath() != null) {
                    if (!groupEntityRepository.existsByPath(sub.getPath())) {
                        Map<String, String> params = new HashMap<>();
                        params.put("referenceId", sub.getPath());
                        params.put("referenceType", "sub");
                        
                        errors.add(new Message(
                            Message.MessageType.ERROR,
                                ERROR_VALIDATION_INVALID_REFERENCE,
                            params,
                            List.of("subRefs")
                        ));
                    }
                }
            }
        }

        return errors;
    }
}
