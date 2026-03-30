package de.ebusyness.priceproviderservice.facade.approle;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppPermissionListRestEntity;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppPermissionRestEntity;

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
