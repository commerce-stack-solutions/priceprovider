package io.commercestacksolutions.priceproviderservice.facade.language;

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
import io.commercestacksolutions.commons.mapper.validation.rules.LocalizedFieldValidationRule;
import io.commercestacksolutions.commons.permissionselector.PermissionMatcher;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.*;
import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.priceproviderservice.facade.language.mapper.LanguageEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.language.mapper.LanguageRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.language.restentity.LanguageListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.language.restentity.LanguageRestEntity;
import io.commercestacksolutions.priceproviderservice.service.language.LanguageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LanguageFacadeImpl implements LanguageFacade {

    private static final Logger logger = LoggerFactory.getLogger(LanguageFacadeImpl.class);

    private final LanguageService languageEntityService;
    private final LanguageRestEntityMapper languageRestEntityMapper;
    private final PatchMapper<LanguageRestEntity> languageRestEntityPatchMapper;
    private final LanguageEntityMapper languageEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;
    private final PermissionMatcher permissionMatcher;
    private final AuthorizationContext authorizationContext;

    @Autowired
    public LanguageFacadeImpl(LanguageService languageEntityService,
                              LanguageRestEntityMapper languageRestEntityMapper,
                              PatchMapper<LanguageRestEntity> languageRestEntityPatchMapper,
                              LanguageEntityMapper languageEntityMapper,
                              EntityMetaInfoRegistry entityMetaInfoRegistry,
                              PermissionMatcher permissionMatcher,
                              AuthorizationContext authorizationContext) {
        this.languageEntityService = languageEntityService;
        this.languageRestEntityMapper = languageRestEntityMapper;
        this.languageRestEntityPatchMapper = languageRestEntityPatchMapper;
        this.languageEntityMapper = languageEntityMapper;

        // Initialize patch validator with validation rules
        // Note: getMandatoryLanguageCodes is passed as a method reference and will be invoked
        // dynamically during each validation, ensuring mandatory languages are always current
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("isoKey")),
                new LocalizedFieldValidationRule(Set.of("name"), this::getMandatoryLanguageCodes)
        ));
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;
        this.permissionMatcher = permissionMatcher;
        this.authorizationContext = authorizationContext;
    }

    /**
     * Retrieves the set of mandatory language codes from the database.
     * This method is called dynamically during PATCH validation to ensure
     * the latest mandatory language settings are always used.
     *
     * @return Set of mandatory language ISO codes retrieved from database
     */
    private Set<String> getMandatoryLanguageCodes() {
        return languageEntityService.getMandatoryLanguages().stream()
                .map(LanguageEntity::getIsoKey)
                .collect(Collectors.toSet());
    }

    /**
     * Checks if the current user has permission to access the given Language entity.
     *
     * @param language the entity to check access for
     * @param action  the action to perform (read, write, delete)
     * @throws AccessDeniedException if the user doesn't have permission
     */
    private void checkAccess(LanguageEntity language, String action) {
        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
        boolean hasAccess = permissionMatcher.hasAccess(permissions, "Language", action, language);

        if (!hasAccess) {
            logger.warn("Access denied for action '{}' on Language with id '{}'", action, language.getIsoKey());
            throw new AccessDeniedException("Access denied to Language with id " + language.getIsoKey());
        }
    }

    @Override
    @Transactional
    public LanguageListRestEntity getLanguages(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<LanguageEntity> languagesPage = languageEntityService.getLanguages(page, pageSize, sortBy, sortDirection, query);
        
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        PagingInfo pagingInfo = new PagingInfo(languagesPage.getNumber(), languagesPage.getSize(), languagesPage.getTotalElements(), languagesPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<LanguageRestEntity> languageRestEntities = languageRestEntityMapper.convertAll(languagesPage.getContent(), context);
        LanguageListRestEntity result = new LanguageListRestEntity(pagingInfo, sortingInfo, languageRestEntities);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(LanguageEntity.class));
        }

        return result;
    }

    @Transactional
    public LanguageRestEntity getLanguage(String isoKey, Set<String> includes) throws NotFoundException, DataMappingException {
        LanguageEntity language = languageEntityService.getLanguage(isoKey);
        if (language == null) {
            Map<String, String> params = new HashMap<>();
            params.put("isoKey", isoKey);
            throw new NotFoundException(MessageKeys.ERROR_LANGUAGE_NOT_FOUND, params);
        }

        // Check read permission
        checkAccess(language, "read");

        RestResponseMappingContext context = new RestResponseMappingContext();
        if (includes != null) {
            context.addExpandPaths(includes);
        }

        LanguageRestEntity result = languageRestEntityMapper.convert(language, context);

        // Add metadata if requested
        if (includes != null && includes.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(LanguageEntity.class));
        }

        return result;

    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(LanguageEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public LanguageRestEntity patch(String isoKey, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify the isoKey field
        List<Message> patchValidationErrors = patchValidator.validate(patch, isoKey);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, errorResponse);
        }

        LanguageRestEntity language = getLanguage(isoKey, Collections.emptySet());
        language = languageRestEntityPatchMapper.applyPatch(patch, language);

        // Fetch existing entity to preserve timestamps and update in place
        LanguageEntity existingLanguage = languageEntityService.getLanguage(isoKey);
        if (existingLanguage == null) {
            Map<String, String> params = new HashMap<>();
            params.put("isoKey", isoKey);
            throw new NotFoundException(MessageKeys.ERROR_LANGUAGE_NOT_FOUND, params);
        }

        // Check write permission
        checkAccess(existingLanguage, "write");

        languageEntityMapper.convert(language, existingLanguage, new RestRequestMappingContext<>(isoKey));
        LanguageEntity saved = languageEntityService.save(existingLanguage);
        return languageRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public LanguageRestEntity createOrReCreate(String isoKey, LanguageRestEntity languageRestEntity) throws DataMappingException, EntityValidationException {
        LanguageEntity language = languageEntityService.getLanguage(isoKey);
        if (language != null) {
            // Update existing language

            // Check write permission before updating
            checkAccess(language, "write");

            languageEntityMapper.convert(languageRestEntity, language, new RestRequestMappingContext<>(isoKey));
            LanguageEntity saved = languageEntityService.save(language);
            return languageRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new language with the isoKey from the path
            LanguageEntity newLanguage = languageEntityMapper.convert(languageRestEntity, new RestRequestMappingContext<>(isoKey));

            // Check write permission for new entity
            checkAccess(newLanguage, "write");

            LanguageEntity saved = languageEntityService.save(newLanguage);
            return languageRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class})
    public LanguageRestEntity create(LanguageRestEntity languageRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        if (languageRestEntity.getIsoKey() == null || languageRestEntity.getIsoKey().isEmpty()) {
            Message message = MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "isoKey", List.of("isoKey"));
            throw new EntityValidationException(MessageKeys.ERROR_VALIDATION_ID_REQUIRED, message);
        }

        // Check if language already exists
        LanguageEntity existingLanguage = languageEntityService.getLanguage(languageRestEntity.getIsoKey());
        if (existingLanguage != null) {
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_LANGUAGE_ALREADY_EXISTS, Map.of("isoKey", languageRestEntity.getIsoKey()), List.of("isoKey"));
        }

        LanguageEntity newLanguage = languageEntityMapper.convert(languageRestEntity, new RestRequestMappingContext<>(languageRestEntity.getIsoKey()));
        LanguageEntity saved = languageEntityService.save(newLanguage);
        return languageRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    public LanguageListRestEntity createOrUpdateAllLanguages(List<LanguageRestEntity> languageRestEntities) {
        if (languageRestEntities == null || languageRestEntities.isEmpty()) {
            LanguageListRestEntity result = new LanguageListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_REQUEST_BODY_EMPTY, "entityType", "Language"));
            return result;
        }

        if (languageRestEntities.size() > 100) {
            LanguageListRestEntity result = new LanguageListRestEntity(null, null, List.of());
            Map<String, String> params = new HashMap<>();
            params.put("maxItems", "100");
            params.put("entityType", "Language");
            result.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED, params, null));
            return result;
        }

        List<LanguageRestEntity> results = new java.util.ArrayList<>();

        for (LanguageRestEntity restEntity : languageRestEntities) {
            try {
                if (restEntity.getIsoKey() == null || restEntity.getIsoKey().isEmpty()) {
                    LanguageRestEntity errorEntity = new LanguageRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "isoKey", List.of("isoKey")));
                    results.add(errorEntity);
                    continue;
                }

                LanguageEntity existingLanguage = languageEntityService.getLanguage(restEntity.getIsoKey());
                if (existingLanguage != null) {
                    // Update existing
                    checkAccess(existingLanguage, "write");

                    languageEntityMapper.convert(restEntity, existingLanguage, new RestRequestMappingContext<>(restEntity.getIsoKey()));
                    LanguageEntity saved = languageEntityService.save(existingLanguage);
                    results.add(languageRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new
                    LanguageEntity newLanguage = languageEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getIsoKey()));
                    LanguageEntity saved = languageEntityService.save(newLanguage);
                    results.add(languageRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                LanguageRestEntity errorEntity = new LanguageRestEntity();
                errorEntity.setIsoKey(restEntity.getIsoKey());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                LanguageRestEntity errorEntity = new LanguageRestEntity();
                errorEntity.setIsoKey(restEntity.getIsoKey());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing language with key {}: {}", restEntity.getIsoKey(), e.getMessage(), e);
                LanguageRestEntity errorEntity = new LanguageRestEntity();
                errorEntity.setIsoKey(restEntity.getIsoKey());
                errorEntity.addMessage(MessageBuilder.create(
                        Message.MessageType.ERROR,
                        MessageKeys.ERROR_PROCESSING,
                        "entity", "language"
                ));
                results.add(errorEntity);
            }
        }

        return new LanguageListRestEntity(null, null, results);
    }

    @Transactional
    public void deleteLanguage(String isoKey) throws NotFoundException {
        LanguageEntity language = languageEntityService.getLanguage(isoKey);
        if (language == null) {
            Map<String, String> params = new HashMap<>();
            params.put("isoKey", isoKey);
            throw new NotFoundException(MessageKeys.ERROR_LANGUAGE_NOT_FOUND, params, List.of("isoKey"));
        }

        // Check delete permission
        checkAccess(language, "delete");

        languageEntityService.deleteLanguage(isoKey);
    }

    public void bulkDeleteLanguages(List<String> isoKeys) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();

        for (String isoKey : isoKeys) {
            LanguageEntity language = languageEntityService.getLanguage(isoKey);
            if (language != null) {
                try {
                    // Check delete permission
                    checkAccess(language, "delete");

                    languageEntityService.deleteLanguage(isoKey);
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                    failedDeletes.add(isoKey);
                } catch (AccessDeniedException ex) {
                    // Rethrow security exceptions - they should not be caught as constraint violations
                    throw ex;
                } catch (Exception ex) {
                    // Check for DataIntegrityViolationException in the cause chain
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof org.springframework.dao.DataIntegrityViolationException) {
                            failedDeletes.add(isoKey);
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
}
