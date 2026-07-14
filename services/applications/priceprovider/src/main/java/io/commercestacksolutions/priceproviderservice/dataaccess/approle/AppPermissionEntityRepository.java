package io.commercestacksolutions.priceproviderservice.dataaccess.approle;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.CommonAppPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AppPermissionEntityRepository extends JpaRepository<CommonAppPermission, Long>, JpaSpecificationExecutor<CommonAppPermission> {

    Optional<CommonAppPermission> findByName(String name);
}
