package de.ebusyness.priceproviderservice.service.organization.validation;

import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.dataaccess.group.GroupEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys.ERROR_VALIDATION_INVALID_REFERENCE;

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
                if (parent != null && parent.getId() != null) {
                    if (!groupEntityRepository.existsById(parent.getId())) {
                        Map<String, String> params = new HashMap<>();
                        params.put("referenceId", parent.getId());
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
                if (sub != null && sub.getId() != null) {
                    if (!groupEntityRepository.existsById(sub.getId())) {
                        Map<String, String> params = new HashMap<>();
                        params.put("referenceId", sub.getId());
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
