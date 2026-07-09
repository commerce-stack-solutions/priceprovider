package io.commercestacksolutions.priceproviderservice.service.approle.validation;

import io.commercestacksolutions.commons.permissionselector.PermissionNameParser;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validates that AppPermission names have valid selector syntax.
 *
 * <p>This rule ensures that:
 * <ul>
 *   <li>Permission name follows the correct format</li>
 *   <li>Selector expressions (if present) are syntactically valid</li>
 *   <li>Backwards compatibility: permissions without selectors remain valid</li>
 * </ul>
 */
@Component
public class AppPermissionSelectorValidationRule implements ValidationRule<AppPermissionEntity> {

    private final PermissionNameParser permissionNameParser = new PermissionNameParser();

    @Override
    public List<Message> validate(AppPermissionEntity entity) {
        List<Message> errors = new ArrayList<>();

        if (entity == null) {
            errors.add(new Message(Message.MessageType.ERROR, "Entity cannot be null", Collections.singletonList("entity")));
            return errors;
        }

        String permissionName = entity.getName();
        if (permissionName == null || permissionName.trim().isEmpty()) {
            // Let the @MandatoryField validation handle this
            return errors;
        }

        // Validate permission name format and selector syntax
        try {
            permissionNameParser.parse(permissionName);
        } catch (IllegalArgumentException e) {
            errors.add(new Message(Message.MessageType.ERROR,
                    "Invalid permission name or selector syntax: " + e.getMessage(),
                    Collections.singletonList("name")));
        }

        return errors;
    }
}
