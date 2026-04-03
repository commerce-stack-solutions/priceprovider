package io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity;

import io.commercestacksolutions.commons.dataaccess.meta.MetaMandatoryField;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.enums.OrganizationType;
import jakarta.persistence.*;

@Entity
public class OrganizationEntity extends GroupEntity {
    
    @Enumerated(EnumType.STRING)
    @MetaMandatoryField
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
