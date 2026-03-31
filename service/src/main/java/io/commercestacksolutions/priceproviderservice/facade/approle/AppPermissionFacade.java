package io.commercestacksolutions.priceproviderservice.facade.approle;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppPermissionListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppPermissionRestEntity;

import java.util.List;
import java.util.Set;

public interface AppPermissionFacade {
    AppPermissionListRestEntity getAppPermissions(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    AppPermissionRestEntity getAppPermission(String id, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    AppPermissionRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    AppPermissionRestEntity createOrRecreate(String id, AppPermissionRestEntity restEntity) throws DataMappingException, EntityValidationException;
    AppPermissionRestEntity create(AppPermissionRestEntity restEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    void delete(String id) throws NotFoundException;
    void bulkDeleteAppPermissions(List<String> ids) throws DataIntegrityException;
    AppPermissionListRestEntity createOrUpdateAllAppPermissions(List<AppPermissionRestEntity> restEntities);
}
