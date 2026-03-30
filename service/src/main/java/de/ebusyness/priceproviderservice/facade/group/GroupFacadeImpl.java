package de.ebusyness.priceproviderservice.facade.group;

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
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.ErrorResponse;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.commons.web.rest.MessageBuilder;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.commons.web.rest.PagingInfo;
import de.ebusyness.commons.web.rest.SortingInfo;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.facade.group.mapper.GroupEntityMapper;
import de.ebusyness.priceproviderservice.facade.group.mapper.GroupRestEntityMapper;
import de.ebusyness.priceproviderservice.facade.group.restentity.GroupListRestEntity;
import de.ebusyness.priceproviderservice.facade.group.restentity.GroupRestEntity;
import de.ebusyness.priceproviderservice.service.group.GroupService;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import de.ebusyness.commons.dataaccess.meta.EntityMetaInfoRegistry;

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

    @Autowired
    public GroupFacadeImpl(GroupService groupEntityService,
                       GroupRestEntityMapper groupRestEntityMapper,
                       PatchMapper<GroupRestEntity> groupRestEntityPatchMapper,
                       GroupEntityMapper groupEntityMapper,
                       EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.groupEntityService = groupEntityService;
        this.groupRestEntityMapper = groupRestEntityMapper;
        this.groupRestEntityPatchMapper = groupRestEntityPatchMapper;
        this.groupEntityMapper = groupEntityMapper;
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;

        // Initialize patch validator with validation rules
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("id"))
        ));
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
        groupEntityMapper.convert(group, existingGroup, new RestRequestMappingContext<>(id));
        GroupEntity saved = groupEntityService.save(existingGroup);
        return groupRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public GroupRestEntity createOrRecreate(String id, GroupRestEntity groupRestEntity) throws DataMappingException, EntityValidationException {
        GroupEntity group = groupEntityService.getGroup(id);
        if (group != null) {
            // Update existing group
            groupEntityMapper.convert(groupRestEntity, group, new RestRequestMappingContext<>(id));
            GroupEntity saved = groupEntityService.save(group);
            return groupRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new group with the id from the path
            GroupEntity newGroup = groupEntityMapper.convert(groupRestEntity, new RestRequestMappingContext<>(id));
            GroupEntity saved = groupEntityService.save(newGroup);
            return groupRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class})
    public GroupRestEntity create(GroupRestEntity groupRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        if (groupRestEntity.getId() == null || groupRestEntity.getId().isEmpty()) {
            Message message = MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "id", List.of("id"));
            throw new EntityValidationException(MessageKeys.ERROR_VALIDATION_ID_REQUIRED, message);
        }

        // Check if group already exists
        GroupEntity existingGroup = groupEntityService.getGroup(groupRestEntity.getId());
        if (existingGroup != null) {
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_GROUP_ALREADY_EXISTS, Map.of("id", groupRestEntity.getId()), List.of("id"));
        }

        GroupEntity newGroup = groupEntityMapper.convert(groupRestEntity, new RestRequestMappingContext<>(groupRestEntity.getId()));
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

        groupEntityService.deleteGroup(id);
    }

    public void bulkDeleteGroups(List<String> ids) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();
        
        for (String id : ids) {
            GroupEntity group = groupEntityService.getGroup(id);
            if (group != null) {
                try {
                    groupEntityService.deleteGroup(id);
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
                if (restEntity.getId() == null || restEntity.getId().isEmpty()) {
                    GroupRestEntity errorEntity = new GroupRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "id", List.of("id")));
                    results.add(errorEntity);
                    continue;
                }

                GroupEntity existingGroup = groupEntityService.getGroup(restEntity.getId());
                if (existingGroup != null) {
                    // Update existing
                    groupEntityMapper.convert(restEntity, existingGroup, new RestRequestMappingContext<>(restEntity.getId()));
                    GroupEntity saved = groupEntityService.save(existingGroup);
                    results.add(groupRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new
                    GroupEntity newGroup = groupEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getId()));
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
                logger.debug("Error processing group with id {}: {}", restEntity.getId(), e.getMessage(), e);
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
