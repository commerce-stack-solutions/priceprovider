package io.commercestacksolutions.priceproviderservice.facade.currency;

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
import io.commercestacksolutions.commons.web.rest.ErrorResponse;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.commons.web.rest.MessageBuilder;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.web.rest.PagingInfo;
import io.commercestacksolutions.commons.web.rest.SortingInfo;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.CountryEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.mapper.CurrencyEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.currency.mapper.CurrencyRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
import io.commercestacksolutions.priceproviderservice.service.currency.CurrencyService;
import io.commercestacksolutions.priceproviderservice.service.language.LanguageService;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CurrencyFacadeImpl implements CurrencyFacade {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyFacadeImpl.class);

    private final CurrencyService currencyEntityService;
    private final LanguageService languageEntityService;
    private final CountryEntityRepository countryEntityRepository;
    private final CurrencyRestEntityMapper currencyRestEntityMapper;
    private final PatchMapper<CurrencyRestEntity> currencyRestEntityPatchMapper;
    private final CurrencyEntityMapper currencyEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;
    private final PermissionMatcher permissionMatcher;
    private final AuthorizationContext authorizationContext;

    @Autowired
    public CurrencyFacadeImpl(CurrencyService currencyEntityService,
                          LanguageService languageEntityService,
                          CountryEntityRepository countryEntityRepository,
                          CurrencyRestEntityMapper currencyRestEntityMapper,
                          PatchMapper<CurrencyRestEntity> currencyRestEntityPatchMapper,
                          CurrencyEntityMapper currencyEntityMapper,
                          EntityMetaInfoRegistry entityMetaInfoRegistry,
                          PermissionMatcher permissionMatcher,
                          AuthorizationContext authorizationContext) {
        this.currencyEntityService = currencyEntityService;
        this.languageEntityService = languageEntityService;
        this.countryEntityRepository = countryEntityRepository;
        this.currencyRestEntityMapper = currencyRestEntityMapper;
        this.currencyRestEntityPatchMapper = currencyRestEntityPatchMapper;
        this.currencyEntityMapper = currencyEntityMapper;

        // Initialize patch validator with validation rules
        // Note: getMandatoryLanguageCodes is passed as a method reference and will be invoked
        // dynamically during each validation, ensuring mandatory languages are always current
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("currencyKey")),
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
     * Checks if the current user has permission to access the given Currency entity.
     *
     * @param currency the entity to check access for
     * @param action   the action to perform (read, write, delete)
     * @throws AccessDeniedException if the user doesn't have permission
     */
    private void checkAccess(CurrencyEntity currency, String action) {
        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
        boolean hasAccess = permissionMatcher.hasAccess(permissions, "Currency", action, currency);

        if (!hasAccess) {
            logger.warn("Access denied for action '{}' on Currency with currencyKey '{}'", action, currency.getCurrencyKey());
            throw new AccessDeniedException("Access denied to Currency with currencyKey " + currency.getCurrencyKey());
        }
    }

    @Transactional
    @Override
    public CurrencyListRestEntity getCurrencies(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<CurrencyEntity> currenciesPage = currencyEntityService.getCurrencies(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        PagingInfo pagingInfo = new PagingInfo(currenciesPage.getNumber(), currenciesPage.getSize(), currenciesPage.getTotalElements(), currenciesPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<CurrencyRestEntity> currencyRestEntities = currencyRestEntityMapper.convertAll(currenciesPage.getContent(), context);
        CurrencyListRestEntity result = new CurrencyListRestEntity(pagingInfo, sortingInfo, currencyRestEntities);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(CurrencyEntity.class));
        }

        return result;
    }

    @Transactional
    public CurrencyRestEntity getCurrency(String currencyKey, Set<String> expand) throws NotFoundException, DataMappingException {
        CurrencyEntity currency = currencyEntityService.getCurrency(currencyKey);
        if (currency == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", currencyKey);
            throw new NotFoundException(MessageKeys.ERROR_CURRENCY_NOT_FOUND, params);
        }

        // Check read permission
        checkAccess(currency, "read");

        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        CurrencyRestEntity result = currencyRestEntityMapper.convert(currency, context);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(CurrencyEntity.class));
        }

        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(CurrencyEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public CurrencyRestEntity patch(String currencyKey, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify the currencyKey field
        List<Message> patchValidationErrors = patchValidator.validate(patch, currencyKey);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException("Patch validation failed", errorResponse);
        }

        CurrencyRestEntity currency = getCurrency(currencyKey, Collections.emptySet());

        currency = currencyRestEntityPatchMapper.applyPatch(patch, currency);

        // Fetch existing entity to preserve timestamps and update in place
        CurrencyEntity existingCurrency = currencyEntityService.getCurrency(currencyKey);
        if (existingCurrency == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", currencyKey);
            throw new NotFoundException(MessageKeys.ERROR_CURRENCY_NOT_FOUND, params);
        }

        // Check write permission
        checkAccess(existingCurrency, "write");

        currencyEntityMapper.convert(currency, existingCurrency, new RestRequestMappingContext<>(currencyKey));
        CurrencyEntity saved = currencyEntityService.save(existingCurrency);
        return currencyRestEntityMapper.convert(saved, new RestResponseMappingContext());

    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public CurrencyRestEntity createOrRecreate(String currencyKey, CurrencyRestEntity currencyRestEntity) throws DataMappingException, EntityValidationException {
        CurrencyEntity currency = currencyEntityService.getCurrency(currencyKey);
        if (currency != null) {
            // Update existing currency

            // Check write permission before updating
            checkAccess(currency, "write");

            currencyEntityMapper.convert(currencyRestEntity, currency, new RestRequestMappingContext<>(currencyKey));
            CurrencyEntity saved = currencyEntityService.save(currency);
            return currencyRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new currency with the currencyKey from the path
            CurrencyEntity newCurrency = currencyEntityMapper.convert(currencyRestEntity, new RestRequestMappingContext<>(currencyKey));

            // Check write permission for new entity
            checkAccess(newCurrency, "write");

            CurrencyEntity saved = currencyEntityService.save(newCurrency);
            return currencyRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class})
    public CurrencyRestEntity create(CurrencyRestEntity currencyRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        if (currencyRestEntity.getCurrencyKey() == null || currencyRestEntity.getCurrencyKey().isEmpty()) {
            Message message = MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "currencyKey", List.of("currencyKey"));
            throw new EntityValidationException(MessageKeys.ERROR_VALIDATION_ID_REQUIRED, message);
        }

        // Check if currency already exists
        CurrencyEntity existingCurrency = currencyEntityService.getCurrency(currencyRestEntity.getCurrencyKey());
        if (existingCurrency != null) {
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_CURRENCY_ALREADY_EXISTS, Map.of("id", currencyRestEntity.getCurrencyKey()), List.of("currencyKey"));
        }

        CurrencyEntity newCurrency = currencyEntityMapper.convert(currencyRestEntity, new RestRequestMappingContext<>(currencyRestEntity.getCurrencyKey()));
        CurrencyEntity saved = currencyEntityService.save(newCurrency);
        return currencyRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    public void delete(String currencyKey) throws NotFoundException {
        // Check if there are any price rows referencing this currency
        // Note: We'll need to update this once we change PriceRow to use CurrencyEntity
        CurrencyEntity currency = currencyEntityService.getCurrency(currencyKey);
        if (currency == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", currencyKey);
            throw new NotFoundException(MessageKeys.ERROR_CURRENCY_NOT_FOUND, params);
        }

        // Check delete permission
        checkAccess(currency, "delete");

        // For now, just delete - we'll add reference checking when we update PriceRow
        currencyEntityService.deleteCurrency(currencyKey);
    }

    public void bulkDeleteCurrencies(List<String> currencyKeys) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();
        Set<String> referencedByTypes = new java.util.LinkedHashSet<>();
        Set<String> referencedByIds = new java.util.LinkedHashSet<>();

        for (String currencyKey : currencyKeys) {
            CurrencyEntity currency = currencyEntityService.getCurrency(currencyKey);
            if (currency == null) {
                continue;
            }

            try {
                // Check delete permission
                checkAccess(currency, "delete");
            } catch (AccessDeniedException ex) {
                // Skip this currency if access is denied, but don't add to failedDeletes
                // (access denied is a different error than data integrity)
                continue;
            }

            // Proactively check if the currency is referenced by any country before attempting delete.
            // This gives a precise, user-friendly error indicating which entity type and which specific
            // entity IDs hold the reference.
            List<String> referencingCountryKeys = countryEntityRepository.findIsoKeysByCurrencyRef(currency);
            if (!referencingCountryKeys.isEmpty()) {
                failedDeletes.add(currencyKey);
                referencedByTypes.add("Country");
                referencedByIds.addAll(referencingCountryKeys);
                continue;
            }

            try {
                currencyEntityService.deleteCurrency(currencyKey);
            } catch (DataIntegrityViolationException ex) {
                failedDeletes.add(currencyKey);
            } catch (ConstraintViolationException ex) {
                failedDeletes.add(currencyKey);
            } catch (AccessDeniedException ex) {
                // Rethrow security exceptions - they should not be caught as constraint violations
                throw ex;
            } catch (Exception ex) {
                // Check for DataIntegrityViolationException or ConstraintViolationException in the cause chain
                Throwable cause = ex.getCause();
                while (cause != null) {
                    if (cause instanceof DataIntegrityViolationException || cause instanceof ConstraintViolationException) {
                        failedDeletes.add(currencyKey);
                        break;
                    }
                    cause = cause.getCause();
                }
                if (cause == null) {
                    throw ex;
                }
            }
        }

        if (!failedDeletes.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("ids", String.join(", ", failedDeletes));
            if (!referencedByTypes.isEmpty()) {
                params.put("referencedBy", String.join(", ", referencedByTypes));
                params.put("referencedByIds", String.join(", ", referencedByIds));
                throw new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED_BY_ENTITY, params, List.of("allowedCurrencyRefs", "primaryCurrencyRef"));
            }
            throw new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
        }
    }

    @Transactional
    public CurrencyListRestEntity createOrUpdateAllCurrencies(List<CurrencyRestEntity> currencyRestEntities) {
        if (currencyRestEntities == null || currencyRestEntities.isEmpty()) {
            CurrencyListRestEntity result = new CurrencyListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_VALIDATION_EMPTY_REQUEST,
                "entity", "currency"
            ));
            return result;
        }

        if (currencyRestEntities.size() > 100) {
            CurrencyListRestEntity result = new CurrencyListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED,
                "maxItems", "100",
                "entity", "currencies"
            ));
            return result;
        }

        List<CurrencyRestEntity> results = new java.util.ArrayList<>();

        for (CurrencyRestEntity restEntity : currencyRestEntities) {
            try {
                if (restEntity.getCurrencyKey() == null || restEntity.getCurrencyKey().isEmpty()) {
                    CurrencyRestEntity errorEntity = new CurrencyRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(
                        Message.MessageType.ERROR,
                        MessageKeys.ERROR_VALIDATION_MANDATORY_FIELD,
                        "field", "currencyKey",
                        List.of("currencyKey")
                    ));
                    results.add(errorEntity);
                    continue;
                }

                CurrencyEntity existingCurrency = currencyEntityService.getCurrency(restEntity.getCurrencyKey());
                if (existingCurrency != null) {
                    // Update existing
                    checkAccess(existingCurrency, "write");

                    currencyEntityMapper.convert(restEntity, existingCurrency, new RestRequestMappingContext<>(restEntity.getCurrencyKey()));
                    CurrencyEntity saved = currencyEntityService.save(existingCurrency);
                    results.add(currencyRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new
                    CurrencyEntity newCurrency = currencyEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getCurrencyKey()));
                    CurrencyEntity saved = currencyEntityService.save(newCurrency);
                    results.add(currencyRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                CurrencyRestEntity errorEntity = new CurrencyRestEntity();
                errorEntity.setCurrencyKey(restEntity.getCurrencyKey());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                CurrencyRestEntity errorEntity = new CurrencyRestEntity();
                errorEntity.setCurrencyKey(restEntity.getCurrencyKey());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing currency with key {}: {}", restEntity.getCurrencyKey(), e.getMessage(), e);
                CurrencyRestEntity errorEntity = new CurrencyRestEntity();
                errorEntity.setCurrencyKey(restEntity.getCurrencyKey());
                errorEntity.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_PROCESSING,
                    "entity", "currency"
                ));
                results.add(errorEntity);
            }
        }

        return new CurrencyListRestEntity(null, null, results);
    }
}
