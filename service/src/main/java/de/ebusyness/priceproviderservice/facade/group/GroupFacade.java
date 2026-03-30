package de.ebusyness.priceproviderservice.facade.group;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.group.restentity.GroupListRestEntity;
import de.ebusyness.priceproviderservice.facade.group.restentity.GroupRestEntity;
import java.util.List;
import java.util.Set;

public interface GroupFacade {
    GroupListRestEntity getGroups(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    GroupRestEntity getGroup(String id, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    GroupRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    GroupRestEntity createOrRecreate(String id, GroupRestEntity groupRestEntity) throws DataMappingException, EntityValidationException;
    GroupRestEntity create(GroupRestEntity groupRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    void delete(String id) throws NotFoundException;
    void bulkDeleteGroups(List<String> ids) throws DataIntegrityException;
    GroupListRestEntity createOrUpdateAllGroups(List<GroupRestEntity> groupRestEntities);
}
