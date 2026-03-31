package io.commercestacksolutions.priceproviderservice.dataaccess.organization;

import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationEntityRepository extends JpaRepository<OrganizationEntity, String>, JpaSpecificationExecutor<OrganizationEntity> {
}
