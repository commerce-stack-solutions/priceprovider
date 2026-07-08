package io.commercestacksolutions.priceproviderservice.dataaccess.approle;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AppPermissionEntityRepository extends JpaRepository<AppPermissionEntity, Long>, JpaSpecificationExecutor<AppPermissionEntity> {

    Optional<AppPermissionEntity> findByName(String name);
}
