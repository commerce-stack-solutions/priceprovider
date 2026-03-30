package de.ebusyness.priceproviderservice.facade.approle;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.dataaccess.meta.EntityMetaInfoRegistry;
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
import de.ebusyness.commons.web.rest.*;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import de.ebusyness.priceproviderservice.facade.approle.mapper.AppPermissionEntityMapper;
import de.ebusyness.priceproviderservice.facade.approle.mapper.AppPermissionRestEntityMapper;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppPermissionListRestEntity;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppPermissionRestEntity;
import de.ebusyness.priceproviderservice.service.approle.AppPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

@Service
public class AppPermissionFacadeImpl implements AppPermissionFacade {

    private static final Logger logger = LoggerFactory.getLogger(AppPermissionFacadeImpl.class);

    public static final int CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS = 100;

    private final AppPermissionService appPermissionService;
    private final AppPermissionRestEntityMapper appPermissionRestEntityMapper;
    private final PatchMapper<AppPermissionRestEntity> appPermissionRestEntityPatchMapper;
    private final AppPermissionEntityMapper appPermissionEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;

    @Autowired
    public AppPermissionFacadeImpl(AppPermissionService appPermissionService,
                                   AppPermissionRestEntityMapper appPermissionRestEntityMapper,
                                   PatchMapper<AppPermissionRestEntity> appPermissionRestEntityPatchMapper,
                                   AppPermissionEntityMapper appPermissionEntityMapper,
                                   EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.appPermissionService = appPermissionService;
        this.appPermissionRestEntityMapper = appPermissionRestEntityMapper;
        this.appPermissionRestEntityPatchMapper = appPermissionRestEntityPatchMapper;
        this.appPermissionEntityMapper = appPermissionEntityMapper;
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;

        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("id"))
        ));
    }

    @Transactional(readOnly = true)
    @Override
    public AppPermissionListRestEntity getAppPermissions(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<AppPermissionEntity> permissionsPage = appPermissionService.getAppPermissions(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        PagingInfo pagingInfo = new PagingInfo(permissionsPage.getNumber(), permissionsPage.getSize(), permissionsPage.getTotalElements(), permissionsPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<AppPermissionRestEntity> restEntities = appPermissionRestEntityMapper.convertAll(permissionsPage.getContent(), context);
        AppPermissionListRestEntity result = new AppPermissionListRestEntity(pagingInfo, sortingInfo, restEntities);

        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(AppPermissionEntity.class));
        }

        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(AppPermissionEntity.class);
    }

    @Transactional
    @Override
    public AppPermissionRestEntity getAppPermission(String id, Set<String> expand) throws NotFoundException, DataMappingException {
        AppPermissionEntity entity = appPermissionService.getAppPermission(id);
        if (entity == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppPermission");
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_APPPERMISSION_NOT_FOUND, params);
        }
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        AppPermissionRestEntity result = appPermissionRestEntityMapper.convert(entity, context);

        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(AppPermissionEntity.class));
        }

        return result;
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public AppPermissionRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        List<Message> patchValidationErrors = patchValidator.validate(patch, id);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, null, errorResponse);
        }

        AppPermissionRestEntity restEntity = getAppPermission(id, Collections.emptySet());
        restEntity = appPermissionRestEntityPatchMapper.applyPatch(patch, restEntity);

        AppPermissionEntity existingEntity = appPermissionService.getAppPermission(id);
        if (existingEntity == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppPermission");
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_APPPERMISSION_NOT_FOUND, params);
        }
        appPermissionEntityMapper.convert(restEntity, existingEntity, new RestRequestMappingContext<>(id));
        AppPermissionEntity saved = appPermissionService.save(existingEntity);
        return appPermissionRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public AppPermissionRestEntity createOrRecreate(String id, AppPermissionRestEntity restEntity) throws DataMappingException, EntityValidationException {
        AppPermissionEntity entity = appPermissionService.getAppPermission(id);
        if (entity != null) {
            appPermissionEntityMapper.convert(restEntity, entity, new RestRequestMappingContext<>(id));
            AppPermissionEntity saved = appPermissionService.save(entity);
            return appPermissionRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            AppPermissionEntity newEntity = appPermissionEntityMapper.convert(restEntity, new RestRequestMappingContext<>(id));
            AppPermissionEntity saved = appPermissionService.save(newEntity);
            return appPermissionRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class})
    @Override
    public AppPermissionRestEntity create(AppPermissionRestEntity restEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        if (restEntity.getId() == null || restEntity.getId().isEmpty()) {
            Message message = MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "id", List.of("id"));
            throw new EntityValidationException(MessageKeys.ERROR_VALIDATION_ID_REQUIRED, message);
        }

        AppPermissionEntity existing = appPermissionService.getAppPermission(restEntity.getId());
        if (existing != null) {
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_APPPERMISSION_ALREADY_EXISTS, Map.of("id", restEntity.getId()), List.of("id"));
        }

        AppPermissionEntity newEntity = appPermissionEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getId()));
        AppPermissionEntity saved = appPermissionService.save(newEntity);
        return appPermissionRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    @Override
    public void delete(String id) throws NotFoundException {
        AppPermissionEntity entity = appPermissionService.getAppPermission(id);
        if (entity == null) {
            Map<String, String> params = new HashMap<>();
            params.put("entityType", "AppPermission");
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_APPPERMISSION_NOT_FOUND, params);
        }
        appPermissionService.deleteAppPermission(id);
    }

    @Override
    public void bulkDeleteAppPermissions(List<String> ids) throws DataIntegrityException {
        List<String> failedDeletes = new ArrayList<>();

        for (String id : ids) {
            AppPermissionEntity entity = appPermissionService.getAppPermission(id);
            if (entity != null) {
                try {
                    appPermissionService.deleteAppPermission(id);
                } catch (DataIntegrityViolationException ex) {
                    failedDeletes.add(id);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof DataIntegrityViolationException || cause instanceof SQLIntegrityConstraintViolationException) {
                            failedDeletes.add(id);
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
            params.put("entityType", "AppPermission");
            params.put("ids", String.join(", ", failedDeletes));
            throw new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
        }
    }

    @Transactional
    @Override
    public AppPermissionListRestEntity createOrUpdateAllAppPermissions(List<AppPermissionRestEntity> restEntities) {
        if (restEntities == null || restEntities.isEmpty()) {
            AppPermissionListRestEntity result = new AppPermissionListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_REQUEST_BODY_EMPTY, "entityType", "AppPermission"));
            return result;
        }

        if (restEntities.size() > CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS) {
            AppPermissionListRestEntity result = new AppPermissionListRestEntity(null, null, List.of());
            Map<String, String> params = new HashMap<>();
            params.put("maxItems", String.valueOf(CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS));
            params.put("entityType", "AppPermission");
            result.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED, params, null));
            return result;
        }

        List<AppPermissionRestEntity> results = new ArrayList<>();

        for (AppPermissionRestEntity restEntity : restEntities) {
            try {
                if (restEntity.getId() == null || restEntity.getId().isEmpty()) {
                    AppPermissionRestEntity errorEntity = new AppPermissionRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "id", List.of("id")));
                    results.add(errorEntity);
                    continue;
                }

                AppPermissionEntity existing = appPermissionService.getAppPermission(restEntity.getId());
                if (existing != null) {
                    appPermissionEntityMapper.convert(restEntity, existing, new RestRequestMappingContext<>(restEntity.getId()));
                    AppPermissionEntity saved = appPermissionService.save(existing);
                    results.add(appPermissionRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    AppPermissionEntity newEntity = appPermissionEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getId()));
                    AppPermissionEntity saved = appPermissionService.save(newEntity);
                    results.add(appPermissionRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                AppPermissionRestEntity errorEntity = new AppPermissionRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                AppPermissionRestEntity errorEntity = new AppPermissionRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing AppPermission with id {}: {}", restEntity.getId(), e.getMessage(), e);
                AppPermissionRestEntity errorEntity = new AppPermissionRestEntity();
                errorEntity.setId(restEntity.getId());
                errorEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_PROCESSING, "entity", "AppPermission"));
                results.add(errorEntity);
            }
        }

        return new AppPermissionListRestEntity(null, null, results);
    }
}
