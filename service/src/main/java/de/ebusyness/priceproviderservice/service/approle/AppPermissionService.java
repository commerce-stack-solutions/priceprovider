package de.ebusyness.priceproviderservice.service.approle;

import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.EntityService;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AppPermissionService extends EntityService<AppPermissionEntity> {
    List<AppPermissionEntity> getAllAppPermissions();
    Page<AppPermissionEntity> getAppPermissions(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    Optional<AppPermissionEntity> getAppPermissionById(String id);
    AppPermissionEntity getAppPermission(String id);
    AppPermissionEntity updateAppPermission(AppPermissionEntity entity) throws EntityValidationException;
    void deleteAppPermission(String id);
}
