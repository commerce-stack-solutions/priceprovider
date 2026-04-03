package io.commercestacksolutions.priceproviderservice.service.approle;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.EntityService;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AppPermissionService extends EntityService<AppPermissionEntity> {
    List<AppPermissionEntity> getAllAppPermissions();
    Page<AppPermissionEntity> getAppPermissions(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    Optional<AppPermissionEntity> getAppPermissionByName(String name);
    AppPermissionEntity getAppPermission(String name);
    AppPermissionEntity updateAppPermission(AppPermissionEntity entity) throws EntityValidationException;
    void deleteAppPermission(String name);
}
