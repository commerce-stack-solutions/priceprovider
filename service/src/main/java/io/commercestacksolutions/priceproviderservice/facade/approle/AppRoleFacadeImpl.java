package io.commercestacksolutions.priceproviderservice.facade.approle;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
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
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.*;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.mapper.AppRoleEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.approle.mapper.AppRoleRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;
import io.commercestacksolutions.priceproviderservice.service.approle.AppRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.Optional;

@Service
public class AppRoleFacadeImpl implements AppRoleFacade {

    private static final Logger logger = LoggerFactory.getLogger(AppRoleFacadeImpl.class);

    public static final int CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS = 100;

    private final AppRoleService appRoleService;
    private final AppRoleRestEntityMapper appRoleRestEntityMapper;
    private final PatchMapper<AppRoleRestEntity> appRoleRestEntityPatchMapper;
    private final AppRoleEntityMapper appRoleEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;

    @Autowired
    public AppRoleFacadeImpl(AppRoleService appRoleService,
                             AppRoleRestEntityMapper appRoleRestEntityMapper,
                             PatchMapper<AppRoleRestEntity> appRoleRestEntityPatchMapper,
                             AppRoleEntityMapper appRoleEntityMapper,
                             EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.appRoleService = appRoleService;
        this.appRoleRestEntityMapper = appRoleRestEntityMapper;
        this.appRoleRestEntityPatchMapper = appRoleRestEntityPatchMapper;
        this.appRoleEntityMapper = appRoleEntityMapper;
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;

        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("id", "name"))
        ));
    }

    @Transactional(readOnly = true)
    @Override
    public AppRoleListRestEntity getAppRoles(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<AppRoleEntity> rolesPage = appRoleService.getAppRoles(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        PagingInfo pagingInfo = new PagingInfo(rolesPage.getNumber(), rolesPage.getSize(), rolesPage.getTotalElements(), rolesPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<AppRoleRestEntity> restEntities = appRoleRestEntityMapper.convertAll(rolesPage.getContent(), context);
        AppRoleListRestEntity result = new AppRoleListRestEntity(pagingInfo, sortingInfo, restEntities);

        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(AppRoleEntity.class));
        }

        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(AppRoleEntity.class);
    }

    @Transactional
    @Override
    public AppRoleRestEntity getAppRole(Long id, Set<String> expand) throws NotFoundException, DataMappingException {
        AppRoleEntity entity = appRoleService.getAppRole(id);
        if (entity == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppRole");
            params.put("id", String.valueOf(id));
            throw new NotFoundException(MessageKeys.ERROR_APPROLE_NOT_FOUND, params);
        }
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        AppRoleRestEntity result = appRoleRestEntityMapper.convert(entity, context);

        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(AppRoleEntity.class));
        }

        return result;
    }

    @Transactional
    @Override
    public AppRoleRestEntity getAppRoleByName(String name, Set<String> expand) throws NotFoundException, DataMappingException {
        AppRoleEntity entity = appRoleService.getAppRoleWithPermissionsByName(name);
        if (entity == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppRole");
            params.put("name", name);
            throw new NotFoundException(MessageKeys.ERROR_APPROLE_NOT_FOUND, params);
        }
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        AppRoleRestEntity result = appRoleRestEntityMapper.convert(entity, context);

        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(AppRoleEntity.class));
        }

        return result;
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public AppRoleRestEntity patch(Long id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        List<Message> patchValidationErrors = patchValidator.validate(patch, String.valueOf(id));
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, null, errorResponse);
        }

        AppRoleRestEntity restEntity = getAppRole(id, Collections.emptySet());
        restEntity = appRoleRestEntityPatchMapper.applyPatch(patch, restEntity);

        AppRoleEntity existingEntity = appRoleService.getAppRole(id);
        if (existingEntity == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppRole");
            params.put("id", String.valueOf(id));
            throw new NotFoundException(MessageKeys.ERROR_APPROLE_NOT_FOUND, params);
        }
        appRoleEntityMapper.convert(restEntity, existingEntity, new RestRequestMappingContext<>(id));
        AppRoleEntity saved = appRoleService.save(existingEntity);
        return appRoleRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, NotFoundException.class})
    @Override
    public AppRoleRestEntity createOrRecreate(Long id, AppRoleRestEntity restEntity) throws DataMappingException, EntityValidationException, NotFoundException {
        AppRoleEntity entity = appRoleService.getAppRole(id);
        if (entity != null) {
            appRoleEntityMapper.convert(restEntity, entity, new RestRequestMappingContext<>(id));
            AppRoleEntity saved = appRoleService.save(entity);
            return appRoleRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppRole");
            params.put("id", String.valueOf(id));
            throw new NotFoundException(MessageKeys.ERROR_APPROLE_NOT_FOUND, params);
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class})
    @Override
    public AppRoleRestEntity create(AppRoleRestEntity restEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        if (restEntity.getName() == null || restEntity.getName().isEmpty()) {
            Message message = MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "name", List.of("name"));
            throw new EntityValidationException(MessageKeys.ERROR_VALIDATION_ID_REQUIRED, message);
        }

        Optional<AppRoleEntity> existingRole = appRoleService.getAppRoleByName(restEntity.getName());
        if (existingRole.isPresent()) {
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_APPROLE_ALREADY_EXISTS, Map.of("name", restEntity.getName()), List.of("name"));
        }

        AppRoleEntity newEntity = appRoleEntityMapper.convert(restEntity, new RestRequestMappingContext<>(null));
        AppRoleEntity saved = appRoleService.save(newEntity);
        return appRoleRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    @Override
    public void delete(Long id) throws NotFoundException {
        AppRoleEntity entity = appRoleService.getAppRole(id);
        if (entity == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppRole");
            params.put("id", String.valueOf(id));
            throw new NotFoundException(MessageKeys.ERROR_APPROLE_NOT_FOUND, params);
        }
        appRoleService.deleteAppRole(id);
    }

    @Override
    public void bulkDeleteAppRoles(List<Long> ids) throws DataIntegrityException {
        List<String> failedDeletes = new ArrayList<>();

        for (Long id : ids) {
            AppRoleEntity entity = appRoleService.getAppRole(id);
            if (entity != null) {
                try {
                    appRoleService.deleteAppRole(id);
                } catch (DataIntegrityViolationException ex) {
                    failedDeletes.add(String.valueOf(id));
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof DataIntegrityViolationException || cause instanceof SQLIntegrityConstraintViolationException) {
                            failedDeletes.add(String.valueOf(id));
                            break;
                        }
                        cause = cause.getCause();
                    }
                    if (cause == null) {
                        throw ex;
                    }
                }
            }
        }

        if (!failedDeletes.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppRole");
            params.put("ids", String.join(", ", failedDeletes));
            throw new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
        }
    }

    @Transactional
    @Override
    public AppRoleListRestEntity createOrUpdateAllAppRoles(List<AppRoleRestEntity> restEntities) {
        if (restEntities == null || restEntities.isEmpty()) {
            AppRoleListRestEntity result = new AppRoleListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_REQUEST_BODY_EMPTY, "entityType", "AppRole"));
            return result;
        }

        if (restEntities.size() > CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS) {
            AppRoleListRestEntity result = new AppRoleListRestEntity(null, null, List.of());
            Map<String, String> params = new HashMap<>();
            params.put("maxItems", String.valueOf(CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS));
            params.put("entityType", "AppRole");
            result.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED, params, null));
            return result;
        }

        List<AppRoleRestEntity> results = new ArrayList<>();

        for (AppRoleRestEntity restEntity : restEntities) {
            try {
                if (restEntity.getName() == null || restEntity.getName().isEmpty()) {
                    AppRoleRestEntity errorEntity = new AppRoleRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "name", List.of("name")));
                    results.add(errorEntity);
                    continue;
                }

                Optional<AppRoleEntity> existingOpt = appRoleService.getAppRoleByName(restEntity.getName());
                if (existingOpt.isPresent()) {
                    AppRoleEntity existing = existingOpt.get();
                    appRoleEntityMapper.convert(restEntity, existing, new RestRequestMappingContext<>(existing.getId()));
                    AppRoleEntity saved = appRoleService.save(existing);
                    results.add(appRoleRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    AppRoleEntity newEntity = appRoleEntityMapper.convert(restEntity, new RestRequestMappingContext<>(null));
                    AppRoleEntity saved = appRoleService.save(newEntity);
                    results.add(appRoleRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                AppRoleRestEntity errorEntity = new AppRoleRestEntity();
                errorEntity.setName(restEntity.getName());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                AppRoleRestEntity errorEntity = new AppRoleRestEntity();
                errorEntity.setName(restEntity.getName());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing AppRole with name {}: {}", restEntity.getName(), e.getMessage(), e);
                AppRoleRestEntity errorEntity = new AppRoleRestEntity();
                errorEntity.setName(restEntity.getName());
                errorEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_PROCESSING, "entity", "AppRole"));
                results.add(errorEntity);
            }
        }

        return new AppRoleListRestEntity(null, null, results);
    }
}
