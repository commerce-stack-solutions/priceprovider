package io.commercestacksolutions.priceproviderservice.facade.group;

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
import io.commercestacksolutions.commons.permissionselector.PermissionMatcher;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.ErrorResponse;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.commons.web.rest.MessageBuilder;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.commons.web.rest.PagingInfo;
import io.commercestacksolutions.commons.web.rest.SortingInfo;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.facade.group.mapper.GroupEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.group.mapper.GroupRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupRestEntity;
import io.commercestacksolutions.priceproviderservice.service.group.GroupService;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GroupFacadeImpl implements GroupFacade {

    private static final Logger logger = LoggerFactory.getLogger(GroupFacadeImpl.class);
    
    public static final int CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS = 100;
    private final GroupService groupEntityService;
    private final GroupRestEntityMapper groupRestEntityMapper;
    private final PatchMapper<GroupRestEntity> groupRestEntityPatchMapper;
    private final GroupEntityMapper groupEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;
    private final PermissionMatcher permissionMatcher;
    private final AuthorizationContext authorizationContext;

    @Autowired
    public GroupFacadeImpl(GroupService groupEntityService,
                       GroupRestEntityMapper groupRestEntityMapper,
                       PatchMapper<GroupRestEntity> groupRestEntityPatchMapper,
                       GroupEntityMapper groupEntityMapper,
                       EntityMetaInfoRegistry entityMetaInfoRegistry,
                       PermissionMatcher permissionMatcher,
                       AuthorizationContext authorizationContext) {
        this.groupEntityService = groupEntityService;
        this.groupRestEntityMapper = groupRestEntityMapper;
        this.groupRestEntityPatchMapper = groupRestEntityPatchMapper;
        this.groupEntityMapper = groupEntityMapper;
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;

        // Initialize patch validator with validation rules
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("id"))
        ));
        this.permissionMatcher = permissionMatcher;
        this.authorizationContext = authorizationContext;
    }

    /**
     * Checks if the current user has permission to access the given Group entity.
     *
     * @param group  the entity to check access for
     * @param action the action to perform (read, write, delete)
     * @throws AccessDeniedException if the user doesn't have permission
     */
    private void checkAccess(GroupEntity group, String action) {
        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
        boolean hasAccess = permissionMatcher.hasAccess(permissions, "Group", action, group);

        if (!hasAccess) {
            logger.warn("Access denied for action '{}' on Group with id '{}'", action, group.getId());
            throw new AccessDeniedException("Access denied to Group with id " + group.getId());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public GroupListRestEntity getGroups(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<GroupEntity> groupsPage = groupEntityService.getGroups(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        PagingInfo pagingInfo = new PagingInfo(groupsPage.getNumber(), groupsPage.getSize(), groupsPage.getTotalElements(), groupsPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<GroupRestEntity> groupRestEntities = groupRestEntityMapper.convertAll(groupsPage.getContent(), context);
        GroupListRestEntity result = new GroupListRestEntity(pagingInfo, sortingInfo, groupRestEntities);
        
        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(GroupEntity.class));
        }
        
        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(GroupEntity.class);
    }

    @Transactional
    public GroupRestEntity getGroup(String id, Set<String> expand) throws NotFoundException, DataMappingException {
        GroupEntity group = groupEntityService.getGroup(id);
        if (group == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "Group");
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_GROUP_NOT_FOUND, params);
        }

        // Check read permission
        checkAccess(group, "read");

        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        GroupRestEntity result = groupRestEntityMapper.convert(group, context);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(GroupEntity.class));
        }

        return result;
    }


    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public GroupRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify the id field
        List<Message> patchValidationErrors = patchValidator.validate(patch, id);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, null, errorResponse);
        }

        GroupRestEntity group = getGroup(id, Collections.emptySet());

        group = groupRestEntityPatchMapper.applyPatch(patch, group);

        // Fetch existing entity to preserve timestamps and update in place
        GroupEntity existingGroup = groupEntityService.getGroup(id);
        if (existingGroup == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "Group");
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_GROUP_NOT_FOUND, params);
        }

        // Check write permission
        checkAccess(existingGroup, "write");

        groupEntityMapper.convert(group, existingGroup, new RestRequestMappingContext<>(id));
        GroupEntity saved = groupEntityService.save(existingGroup);
        return groupRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public GroupRestEntity createOrRecreate(String id, GroupRestEntity groupRestEntity) throws DataMappingException, EntityValidationException {
        GroupEntity group = groupEntityService.getGroup(id);
        if (group != null) {
            // Update existing group

            // Check write permission before updating
            checkAccess(group, "write");

            groupEntityMapper.convert(groupRestEntity, group, new RestRequestMappingContext<>(id));
            GroupEntity saved = groupEntityService.save(group);
            return groupRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new group using the id provided by the client in the URL
            GroupEntity newGroup = groupEntityMapper.convert(groupRestEntity, new RestRequestMappingContext<>(id));

            // Check write permission for new entity
            checkAccess(newGroup, "write");

            GroupEntity saved = groupEntityService.save(newGroup);
            return groupRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class})
    public GroupRestEntity create(GroupRestEntity groupRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        if (groupRestEntity.getPath() == null || groupRestEntity.getPath().isEmpty()) {
            Message message = MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_PATH_REQUIRED, "field", "path", List.of("path"));
            throw new EntityValidationException(MessageKeys.ERROR_VALIDATION_PATH_REQUIRED, message);
        }

        // Check if group already exists by path
        GroupEntity existingGroup = groupEntityService.getGroupByPath(groupRestEntity.getPath());
        if (existingGroup != null) {
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_GROUP_ALREADY_EXISTS, Map.of("path", groupRestEntity.getPath()), List.of("path"));
        }

        // UUID is auto-generated (no id in context)
        GroupEntity newGroup = groupEntityMapper.convert(groupRestEntity, new RestRequestMappingContext<>(null));
        GroupEntity saved = groupEntityService.save(newGroup);
        return groupRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    public void delete(String id) throws NotFoundException {
        GroupEntity group = groupEntityService.getGroup(id);
        if (group == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "Group");
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_GROUP_NOT_FOUND, params);
        }

        // Check delete permission
        checkAccess(group, "delete");

        groupEntityService.deleteGroup(id);
    }

    public void bulkDeleteGroups(List<String> ids) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();

        for (String id : ids) {
            GroupEntity group = groupEntityService.getGroup(id);
            if (group != null) {
                try {
                    // Check delete permission
                    checkAccess(group, "delete");

                    groupEntityService.deleteGroup(id);
                } catch (DataIntegrityViolationException ex) {
                    failedDeletes.add(id);
                } catch (AccessDeniedException ex) {
                    // Rethrow security exceptions - they should not be caught as constraint violations
                    throw ex;
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
            params.put("entityType", "Group");
            params.put("ids", String.join(", ", failedDeletes));
            throw new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
        }
    }

    @Transactional
    public GroupListRestEntity createOrUpdateAllGroups(List<GroupRestEntity> groupRestEntities) {
        if (groupRestEntities == null || groupRestEntities.isEmpty()) {
            GroupListRestEntity result = new GroupListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_REQUEST_BODY_EMPTY, "entityType", "Group"));
            return result;
        }

        if (groupRestEntities.size() > CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS) {
            GroupListRestEntity result = new GroupListRestEntity(null, null, List.of());
            Map<String, String> params = new HashMap<>();
            params.put("maxItems", String.valueOf(CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS));
            params.put("entityType", "Group");
            result.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED, params, null));
            return result;
        }

        List<GroupRestEntity> results = new java.util.ArrayList<>();

        for (GroupRestEntity restEntity : groupRestEntities) {
            try {
                // Determine if an existing entity can be found (by String id or by path)
                GroupEntity existingGroup = null;
                if (restEntity.getId() != null) {
                    existingGroup = groupEntityService.getGroup(restEntity.getId());
                }
                if (existingGroup == null && restEntity.getPath() != null && !restEntity.getPath().isEmpty()) {
                    existingGroup = groupEntityService.getGroupByPath(restEntity.getPath());
                }

                if (existingGroup == null && (restEntity.getPath() == null || restEntity.getPath().isEmpty())) {
                    GroupRestEntity errorEntity = new GroupRestEntity();
                    errorEntity.setId(restEntity.getId());
                    errorEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_PATH_REQUIRED, "field", "path", List.of("path")));
                    results.add(errorEntity);
                    continue;
                }

                if (existingGroup != null) {
                    // Update existing
                    checkAccess(existingGroup, "write");

                    groupEntityMapper.convert(restEntity, existingGroup, new RestRequestMappingContext<>(existingGroup.getId()));
                    GroupEntity saved = groupEntityService.save(existingGroup);
                    results.add(groupRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new (UUID auto-generated)
                    GroupEntity newGroup = groupEntityMapper.convert(restEntity, new RestRequestMappingContext<>(null));
                    GroupEntity saved = groupEntityService.save(newGroup);
                    results.add(groupRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                GroupRestEntity errorEntity = new GroupRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                GroupRestEntity errorEntity = new GroupRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing group with path {}: {}", restEntity.getPath(), e.getMessage(), e);
                GroupRestEntity errorEntity = new GroupRestEntity();
                errorEntity.setId(restEntity.getId());
                errorEntity.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_PROCESSING,
                    "entity", "group"
                ));
                results.add(errorEntity);
            }
        }

        return new GroupListRestEntity(null, null, results);
    }
}
