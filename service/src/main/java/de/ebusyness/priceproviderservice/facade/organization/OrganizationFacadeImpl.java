package de.ebusyness.priceproviderservice.facade.organization;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.PatchMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.mapper.validation.PatchValidator;
import de.ebusyness.commons.mapper.validation.rules.ImmutableFieldsRule;
import de.ebusyness.commons.query.QueryParser;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.*;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.commons.dataaccess.meta.EntityMetaInfoRegistry;
import de.ebusyness.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import de.ebusyness.priceproviderservice.facade.organization.mapper.OrganizationEntityMapper;
import de.ebusyness.priceproviderservice.facade.organization.mapper.OrganizationRestEntityMapper;
import de.ebusyness.priceproviderservice.facade.organization.restentity.OrganizationListRestEntity;
import de.ebusyness.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
import de.ebusyness.priceproviderservice.service.organization.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrganizationFacadeImpl implements OrganizationFacade {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationFacadeImpl.class);

    public static final int CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS = 100;
    private final OrganizationService organizationEntityService;
    private final OrganizationRestEntityMapper organizationRestEntityMapper;
    private final PatchMapper<OrganizationRestEntity> organizationRestEntityPatchMapper;
    private final OrganizationEntityMapper organizationEntityMapper;
    private final PatchValidator patchValidator;
    private final QueryParser queryParser;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;

    @Autowired
    public OrganizationFacadeImpl(OrganizationService organizationEntityService,
                              OrganizationRestEntityMapper organizationRestEntityMapper,
                              PatchMapper<OrganizationRestEntity> organizationRestEntityPatchMapper,
                              OrganizationEntityMapper organizationEntityMapper,
                              EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.organizationEntityService = organizationEntityService;
        this.organizationRestEntityMapper = organizationRestEntityMapper;
        this.organizationRestEntityPatchMapper = organizationRestEntityPatchMapper;
        this.organizationEntityMapper = organizationEntityMapper;
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;
        this.queryParser = new QueryParser(OrganizationEntity.class);

        // Initialize patch validator with validation rules
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("id"))
        ));
    }

    @Transactional
    @Override
    public OrganizationListRestEntity getOrganizations(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<OrganizationEntity> organizationsPage = organizationEntityService.getOrganizations(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        PagingInfo pagingInfo = new PagingInfo(organizationsPage.getNumber(), organizationsPage.getSize(), organizationsPage.getTotalElements(), organizationsPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<OrganizationRestEntity> organizationRestEntities = organizationRestEntityMapper.convertAll(organizationsPage.getContent(), context);
        OrganizationListRestEntity result = new OrganizationListRestEntity(pagingInfo, sortingInfo, organizationRestEntities);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(OrganizationEntity.class));
        }

        return result;
    }

    @Transactional
    public OrganizationRestEntity getOrganization(String id, Set<String> expand) throws NotFoundException, DataMappingException {
        OrganizationEntity organization = organizationEntityService.getOrganization(id);
        if (organization == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_ORGANIZATION_NOT_FOUND, params);
        }
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        OrganizationRestEntity result = organizationRestEntityMapper.convert(organization, context);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(OrganizationEntity.class));
        }

        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(OrganizationEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public OrganizationRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify the id field
        List<Message> patchValidationErrors = patchValidator.validate(patch, id);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException("Patch validation failed", errorResponse);
        }

        OrganizationRestEntity organization = getOrganization(id, Collections.emptySet());

        organization = organizationRestEntityPatchMapper.applyPatch(patch, organization);
        
        // Fetch existing entity to preserve timestamps and update in place
        OrganizationEntity existingOrganization = organizationEntityService.getOrganization(id);
        if (existingOrganization == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_ORGANIZATION_NOT_FOUND, params);
        }
        organizationEntityMapper.convert(organization, existingOrganization, new RestRequestMappingContext<>(id));
        OrganizationEntity saved = organizationEntityService.save(existingOrganization);
        return organizationRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public OrganizationRestEntity createOrRecreate(String id, OrganizationRestEntity organizationRestEntity) throws DataMappingException, EntityValidationException {
        OrganizationEntity organization = organizationEntityService.getOrganization(id);
        if (organization != null) {
            // Update existing organization
            organizationEntityMapper.convert(organizationRestEntity, organization, new RestRequestMappingContext<>(id));
            OrganizationEntity saved = organizationEntityService.save(organization);
            return organizationRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new organization with the id from the path
            OrganizationEntity newOrganization = organizationEntityMapper.convert(organizationRestEntity, new RestRequestMappingContext<>(id));
            OrganizationEntity saved = organizationEntityService.save(newOrganization);
            return organizationRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class})
    public OrganizationRestEntity create(OrganizationRestEntity organizationRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        if (organizationRestEntity.getId() == null || organizationRestEntity.getId().isEmpty()) {
            Message message = MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "id", List.of("id"));
            throw new EntityValidationException(MessageKeys.ERROR_VALIDATION_ID_REQUIRED, message);
        }

        // Check if organization already exists
        OrganizationEntity existingOrganization = organizationEntityService.getOrganization(organizationRestEntity.getId());
        if (existingOrganization != null) {
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_ORGANIZATION_ALREADY_EXISTS, Map.of("id", organizationRestEntity.getId()), List.of("id"));
        }

        OrganizationEntity newOrganization = organizationEntityMapper.convert(organizationRestEntity, new RestRequestMappingContext<>(organizationRestEntity.getId()));
        OrganizationEntity saved = organizationEntityService.save(newOrganization);
        return organizationRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    public void delete(String id) throws NotFoundException {
        OrganizationEntity organization = organizationEntityService.getOrganization(id);
        if (organization == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_ORGANIZATION_NOT_FOUND, params);
        }

        organizationEntityService.deleteOrganization(id);
    }

    public void bulkDeleteOrganizations(List<String> ids) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();
        
        for (String id : ids) {
            OrganizationEntity organization = organizationEntityService.getOrganization(id);
            if (organization != null) {
                try {
                    organizationEntityService.deleteOrganization(id);
                } catch (DataIntegrityViolationException ex) {
                    failedDeletes.add(id);
                } catch (Exception ex) {
                    // Check for SQLIntegrityConstraintViolationException in the cause chain
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof DataIntegrityViolationException || cause instanceof SQLIntegrityConstraintViolationException) {
                            failedDeletes.add(id);
                            break;
                        }
                        cause = cause.getCause();
                    }
                    if (cause == null) {
                        // Re-throw if not a data integrity issue
                        throw ex;
                    }
                }
            }
        }
        
        if (!failedDeletes.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("ids", String.join(", ", failedDeletes));
            throw new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
        }
    }

    @Transactional
    public OrganizationListRestEntity createOrUpdateAllOrganizations(List<OrganizationRestEntity> organizationRestEntities) {
        if (organizationRestEntities == null || organizationRestEntities.isEmpty()) {
            OrganizationListRestEntity result = new OrganizationListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_VALIDATION_EMPTY_REQUEST,
                "entity", "organization"
            ));
            return result;
        }

        if (organizationRestEntities.size() > CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS) {
            OrganizationListRestEntity result = new OrganizationListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED,
                "maxItems", String.valueOf(CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS),
                "entity", "organizations"
            ));
            return result;
        }

        List<OrganizationRestEntity> results = new java.util.ArrayList<>();

        for (OrganizationRestEntity restEntity : organizationRestEntities) {
            try {
                if (restEntity.getId() == null || restEntity.getId().isEmpty()) {
                    OrganizationRestEntity errorEntity = new OrganizationRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(
                        Message.MessageType.ERROR,
                        MessageKeys.ERROR_VALIDATION_MANDATORY_FIELD,
                        "field", "id",
                        List.of("id")
                    ));
                    results.add(errorEntity);
                    continue;
                }

                OrganizationEntity existingOrganization = organizationEntityService.getOrganization(restEntity.getId());
                if (existingOrganization != null) {
                    // Update existing
                    organizationEntityMapper.convert(restEntity, existingOrganization, new RestRequestMappingContext<>(restEntity.getId()));
                    OrganizationEntity saved = organizationEntityService.save(existingOrganization);
                    results.add(organizationRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new
                    OrganizationEntity newOrganization = organizationEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getId()));
                    OrganizationEntity saved = organizationEntityService.save(newOrganization);
                    results.add(organizationRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                OrganizationRestEntity errorEntity = new OrganizationRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                OrganizationRestEntity errorEntity = new OrganizationRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing organization with id {}: {}", restEntity.getId(), e.getMessage(), e);
                OrganizationRestEntity errorEntity = new OrganizationRestEntity();
                errorEntity.setId(restEntity.getId());
                errorEntity.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_PROCESSING,
                    "entity", "organization"
                ));
                results.add(errorEntity);
            }
        }

        return new OrganizationListRestEntity(null, null, results);
    }
}
