package de.ebusyness.priceproviderservice.dataaccess.organization.entity;

import de.ebusyness.commons.dataaccess.meta.MetaMandatoryField;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.dataaccess.organization.enums.OrganizationType;
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
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", organizationType=" + organizationType +
                ", createdAt=" + getCreatedAt() +
                ", lastModifiedAt=" + getLastModifiedAt() +
                '}';
    }
}
