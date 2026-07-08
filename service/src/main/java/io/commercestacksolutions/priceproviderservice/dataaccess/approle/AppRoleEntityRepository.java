package io.commercestacksolutions.priceproviderservice.dataaccess.approle;

import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppRoleEntityRepository extends JpaRepository<AppRoleEntity, Long>, JpaSpecificationExecutor<AppRoleEntity> {

    Optional<AppRoleEntity> findByName(String name);

	/**
	 * Fetch role along with its permissions to avoid LazyInitializationException when accessed outside a transaction.
	 */
	@Query("select r from AppRoleEntity r left join fetch r.permissionRefs where r.id = :id")
	Optional<AppRoleEntity> findByIdWithPermissions(@Param("id") Long id);

	@Query("select r from AppRoleEntity r left join fetch r.permissionRefs where r.name = :name")
	Optional<AppRoleEntity> findByNameWithPermissions(@Param("name") String name);

}


