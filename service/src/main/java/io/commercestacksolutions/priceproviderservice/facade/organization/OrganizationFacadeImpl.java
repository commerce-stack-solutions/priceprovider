package io.commercestacksolutions.priceproviderservice.facade.organization;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.PatchMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.mapper.validation.PatchValidator;
import io.commercestacksolutions.commons.mapper.validation.rules.ImmutableFieldsRule;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.*;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.priceproviderservice.facade.organization.mapper.OrganizationEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.organization.mapper.OrganizationRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.organization.restentity.OrganizationListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
import io.commercestacksolutions.priceproviderservice.service.organization.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.UUID;
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

    /**
     * Parse a String UUID from the URL path parameter.
     * Returns null if the string is not a valid UUID (treated as not-found).
     */
    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
        UUID uuid = parseUUID(id);
        OrganizationEntity organization = uuid != null ? organizationEntityService.getOrganization(uuid) : null;
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

    @Transactional(readOnly = true)
    public OrganizationRestEntity getOrganizationByPath(String path, Set<String> expand) throws NotFoundException, DataMappingException {
        OrganizationEntity organization = organizationEntityService.getOrganizationByPath(path);
        if (organization == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", path);
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
        UUID uuid = parseUUID(id);
        OrganizationEntity existingOrganization = uuid != null ? organizationEntityService.getOrganization(uuid) : null;
        if (existingOrganization == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_ORGANIZATION_NOT_FOUND, params);
        }
        organizationEntityMapper.convert(organization, existingOrganization, new RestRequestMappingContext<>(uuid));
        OrganizationEntity saved = organizationEntityService.save(existingOrganization);
        return organizationRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public OrganizationRestEntity createOrRecreate(String id, OrganizationRestEntity organizationRestEntity) throws DataMappingException, EntityValidationException {
        UUID uuid = parseUUID(id);
        OrganizationEntity organization = uuid != null ? organizationEntityService.getOrganization(uuid) : null;
        if (organization != null) {
            // Update existing organization
            organizationEntityMapper.convert(organizationRestEntity, organization, new RestRequestMappingContext<>(uuid));
            OrganizationEntity saved = organizationEntityService.save(organization);
            return organizationRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new organization with the UUID from the path (or auto-generated if invalid)
            OrganizationEntity newOrganization = organizationEntityMapper.convert(organizationRestEntity, new RestRequestMappingContext<>(uuid));
            OrganizationEntity saved = organizationEntityService.save(newOrganization);
            return organizationRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class})
    public OrganizationRestEntity create(OrganizationRestEntity organizationRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        if (organizationRestEntity.getPath() == null || organizationRestEntity.getPath().isEmpty()) {
            Message message = MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_PATH_REQUIRED, "field", "path", List.of("path"));
            throw new EntityValidationException(MessageKeys.ERROR_VALIDATION_PATH_REQUIRED, message);
        }

        // Check if organization already exists by path
        OrganizationEntity existingOrganization = organizationEntityService.getOrganizationByPath(organizationRestEntity.getPath());
        if (existingOrganization != null) {
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_ORGANIZATION_ALREADY_EXISTS, Map.of("path", organizationRestEntity.getPath()), List.of("path"));
        }

        // UUID is auto-generated (no id in context)
        OrganizationEntity newOrganization = organizationEntityMapper.convert(organizationRestEntity, new RestRequestMappingContext<>(null));
        OrganizationEntity saved = organizationEntityService.save(newOrganization);
        return organizationRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    public void delete(String id) throws NotFoundException {
        UUID uuid = parseUUID(id);
        OrganizationEntity organization = uuid != null ? organizationEntityService.getOrganization(uuid) : null;
        if (organization == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_ORGANIZATION_NOT_FOUND, params);
        }

        organizationEntityService.deleteOrganization(uuid);
    }

    public void bulkDeleteOrganizations(List<String> ids) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();
        
        for (String id : ids) {
            UUID uuid = parseUUID(id);
            OrganizationEntity organization = uuid != null ? organizationEntityService.getOrganization(uuid) : null;
            if (organization != null) {
                try {
                    organizationEntityService.deleteOrganization(uuid);
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
                // Determine if an existing entity can be found (by UUID id or by path)
                OrganizationEntity existingOrganization = null;
                if (restEntity.getId() != null) {
                    existingOrganization = organizationEntityService.getOrganization(restEntity.getId());
                }
                if (existingOrganization == null && restEntity.getPath() != null && !restEntity.getPath().isEmpty()) {
                    existingOrganization = organizationEntityService.getOrganizationByPath(restEntity.getPath());
                }

                if (existingOrganization == null && (restEntity.getPath() == null || restEntity.getPath().isEmpty())) {
                    OrganizationRestEntity errorEntity = new OrganizationRestEntity();
                    errorEntity.setId(restEntity.getId());
                    errorEntity.addMessage(MessageBuilder.create(
                        Message.MessageType.ERROR,
                        MessageKeys.ERROR_VALIDATION_PATH_REQUIRED,
                        "field", "path",
                        List.of("path")
                    ));
                    results.add(errorEntity);
                    continue;
                }

                if (existingOrganization != null) {
                    // Update existing
                    organizationEntityMapper.convert(restEntity, existingOrganization, new RestRequestMappingContext<>(existingOrganization.getId()));
                    OrganizationEntity saved = organizationEntityService.save(existingOrganization);
                    results.add(organizationRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new (UUID auto-generated)
                    OrganizationEntity newOrganization = organizationEntityMapper.convert(restEntity, new RestRequestMappingContext<>(null));
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
                logger.debug("Error processing organization with path {}: {}", restEntity.getPath(), e.getMessage(), e);
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
