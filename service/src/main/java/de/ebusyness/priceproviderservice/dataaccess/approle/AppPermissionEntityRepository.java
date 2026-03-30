package de.ebusyness.priceproviderservice.dataaccess.approle;

import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AppPermissionEntityRepository extends JpaRepository<AppPermissionEntity, String>, JpaSpecificationExecutor<AppPermissionEntity> {
}
