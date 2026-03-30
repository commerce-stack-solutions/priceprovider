package de.ebusyness.priceproviderservice.facade.organization;

import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.organization.restentity.OrganizationListRestEntity;
import de.ebusyness.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
import java.util.List;
import java.util.Set;

public interface OrganizationFacade {
    OrganizationListRestEntity getOrganizations(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    OrganizationRestEntity getOrganization(String id, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    OrganizationRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    OrganizationRestEntity createOrRecreate(String id, OrganizationRestEntity organizationRestEntity) throws DataMappingException, EntityValidationException;
    OrganizationRestEntity create(OrganizationRestEntity organizationRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    void delete(String id) throws NotFoundException;
    void bulkDeleteOrganizations(List<String> ids) throws DataIntegrityException;
    OrganizationListRestEntity createOrUpdateAllOrganizations(List<OrganizationRestEntity> organizationRestEntities);
}
