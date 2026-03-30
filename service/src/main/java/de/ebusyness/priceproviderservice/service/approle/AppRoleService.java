package de.ebusyness.priceproviderservice.service.approle;

import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.EntityService;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AppRoleService extends EntityService<AppRoleEntity> {
    List<AppRoleEntity> getAllAppRoles();
    Page<AppRoleEntity> getAppRoles(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    Optional<AppRoleEntity> getAppRoleById(String id);
    AppRoleEntity getAppRole(String id);
    AppRoleEntity updateAppRole(AppRoleEntity entity) throws EntityValidationException;
    void deleteAppRole(String id);
}
