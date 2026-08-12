package io.commercestacksolutions.coreserviceapp.dataaccess.approle;

import io.commercestacksolutions.coreserviceapp.dataaccess.approle.entity.CommonAppPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AppPermissionEntityRepository extends JpaRepository<CommonAppPermission, Long>, JpaSpecificationExecutor<CommonAppPermission> {

    Optional<CommonAppPermission> findByName(String name);
}
