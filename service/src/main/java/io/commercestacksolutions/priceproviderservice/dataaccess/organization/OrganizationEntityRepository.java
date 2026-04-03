package io.commercestacksolutions.priceproviderservice.dataaccess.organization;

import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationEntityRepository extends JpaRepository<OrganizationEntity, UUID>, JpaSpecificationExecutor<OrganizationEntity> {

    /**
     * Find an organization by its path (unique human-readable identifier).
     *
     * @param path the unique path of the organization
     * @return optional containing the organization entity if found
     */
    Optional<OrganizationEntity> findByPath(String path);

    /**
     * Check whether an organization with the given path exists.
     *
     * @param path the unique path to check
     * @return true if an organization with this path exists
     */
    boolean existsByPath(String path);
}
