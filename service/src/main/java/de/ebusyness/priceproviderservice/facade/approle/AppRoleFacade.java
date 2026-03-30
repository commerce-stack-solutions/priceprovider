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
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppRoleListRestEntity;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;

import java.util.List;
import java.util.Set;

public interface AppRoleFacade {
    AppRoleListRestEntity getAppRoles(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    AppRoleRestEntity getAppRole(String id, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    AppRoleRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    AppRoleRestEntity createOrRecreate(String id, AppRoleRestEntity restEntity) throws DataMappingException, EntityValidationException;
    AppRoleRestEntity create(AppRoleRestEntity restEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    void delete(String id) throws NotFoundException;
    void bulkDeleteAppRoles(List<String> ids) throws DataIntegrityException;
    AppRoleListRestEntity createOrUpdateAllAppRoles(List<AppRoleRestEntity> restEntities);
}
