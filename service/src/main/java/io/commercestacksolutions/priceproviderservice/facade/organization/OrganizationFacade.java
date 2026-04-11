package io.commercestacksolutions.priceproviderservice.facade.organization;

import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.organization.restentity.OrganizationListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
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
