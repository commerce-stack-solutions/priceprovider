package io.commercestacksolutions.priceproviderservice.service.approle;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.EntityService;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.CommonAppPermission;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AppPermissionService extends EntityService<CommonAppPermission>,
    io.commercestacksolutions.commons.service.approle.CommonAppPermissionService {
    List<CommonAppPermission> getAllAppPermissions();
    Page<CommonAppPermission> getAppPermissions(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    Optional<CommonAppPermission> getAppPermissionById(Long id);
    CommonAppPermission getAppPermission(Long id);
    Optional<CommonAppPermission> getAppPermissionByName(String name);
    CommonAppPermission updateAppPermission(CommonAppPermission entity) throws EntityValidationException;
    void deleteAppPermission(Long id);
}
