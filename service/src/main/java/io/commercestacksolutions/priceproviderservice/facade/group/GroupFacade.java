package io.commercestacksolutions.priceproviderservice.facade.group;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupRestEntity;
import java.util.List;
import java.util.Set;

public interface GroupFacade {
    GroupListRestEntity getGroups(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    GroupRestEntity getGroup(String id, Set<String> expand) throws NotFoundException, DataMappingException;
    GroupRestEntity getGroupByPath(String path, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    GroupRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    GroupRestEntity createOrRecreate(String id, GroupRestEntity groupRestEntity) throws DataMappingException, EntityValidationException;
    GroupRestEntity create(GroupRestEntity groupRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    void delete(String id) throws NotFoundException;
    void bulkDeleteGroups(List<String> ids) throws DataIntegrityException;
    GroupListRestEntity createOrUpdateAllGroups(List<GroupRestEntity> groupRestEntities);
}
