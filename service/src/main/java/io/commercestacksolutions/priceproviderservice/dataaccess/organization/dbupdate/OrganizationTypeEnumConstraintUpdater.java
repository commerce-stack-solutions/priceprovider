package io.commercestacksolutions.priceproviderservice.dataaccess.group.dbupdate;

import io.commercestacksolutions.commons.dataaccess.dbupdate.AbstractEnumConstraintUpdater;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.enums.OrganizationType;
import org.springframework.stereotype.Component;

/**
 * Schema updater for OrganizationType enum constraint.
 * Ensures the database CHECK constraint includes all OrganizationType enum values.
 */
@Component
public class OrganizationTypeEnumConstraintUpdater extends AbstractEnumConstraintUpdater {
    
    @Override
    protected String getTableName() {
        return "organization_entity";
    }
    
    @Override
    protected String getColumnName() {
        return "organization_type";
    }
    
    @Override
    protected Class<? extends Enum<?>> getEnumClass() {
        return OrganizationType.class;
    }
    
    @Override
    public int getPriority() {
        return 101; // Standard priority for organization type enum constraints
    }
}
