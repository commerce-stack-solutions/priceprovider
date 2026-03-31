package io.commercestacksolutions.priceproviderservice.service.approle;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.EntityService;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
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
