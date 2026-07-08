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
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;

import java.util.List;
import java.util.Set;

public interface AppRoleFacade {
    AppRoleListRestEntity getAppRoles(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    AppRoleRestEntity getAppRole(Long id, Set<String> expand) throws NotFoundException, DataMappingException;
    AppRoleRestEntity getAppRoleByName(String name, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    AppRoleRestEntity patch(Long id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    AppRoleRestEntity createOrRecreate(Long id, AppRoleRestEntity restEntity) throws DataMappingException, EntityValidationException, NotFoundException;
    AppRoleRestEntity create(AppRoleRestEntity restEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    void delete(Long id) throws NotFoundException;
    void bulkDeleteAppRoles(List<Long> ids) throws DataIntegrityException;
    AppRoleListRestEntity createOrUpdateAllAppRoles(List<AppRoleRestEntity> restEntities);
}
