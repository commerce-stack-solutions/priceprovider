package io.commercestacksolutions.corebusinessentities.dataaccess.organization.entity;

import io.commercestacksolutions.commons.dataaccess.meta.MandatoryField;
import io.commercestacksolutions.commons.dataaccess.meta.MetaDynamicEnum;
import io.commercestacksolutions.corebusinessentities.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.converter.OrganizationTypeConverter;
import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.OrganizationType;
import io.commercestacksolutions.corebusinessentities.dataaccess.organization.organizationtype.OrganizationTypeDefinition;
import jakarta.persistence.*;

@Entity
public class OrganizationEntity extends GroupEntity {

    @Convert(converter = OrganizationTypeConverter.class)
    @MetaDynamicEnum(beanType = OrganizationTypeDefinition.class)
    @MandatoryField
    private OrganizationType organizationType;

    public OrganizationEntity() {
        super();
    }

    public OrganizationEntity(String id) {
        super(id);
    }

    public OrganizationType getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(OrganizationType organizationType) {
        this.organizationType = organizationType;
    }

    @Override
    public String toString() {
        return "OrganizationEntity{" +
                "id=" + getId() +
                ", path='" + getPath() + '\'' +
                ", name='" + getName() + '\'' +
                ", organizationType=" + organizationType +
                ", createdAt=" + getCreatedAt() +
                ", lastModifiedAt=" + getLastModifiedAt() +
                '}';
    }
}
