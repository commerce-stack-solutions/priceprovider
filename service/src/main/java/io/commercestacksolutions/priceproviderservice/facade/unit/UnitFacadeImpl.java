package io.commercestacksolutions.priceproviderservice.facade.unit;

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
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.*;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.priceproviderservice.facade.unit.mapper.UnitEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.unit.mapper.UnitRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitRestEntity;
import io.commercestacksolutions.priceproviderservice.service.language.LanguageService;
import io.commercestacksolutions.priceproviderservice.service.unit.UnitService;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of UnitFacadeService interface.
 * This class provides concrete implementation for unit facade operations.
 */
@Service
public class UnitFacadeImpl implements UnitFacadeService {

    private static final Logger logger = LoggerFactory.getLogger(UnitFacadeImpl.class);

    private final UnitService unitEntityService;
    private final LanguageService languageEntityService;
    private final UnitRestEntityMapper unitRestEntityMapper;
    private final PatchMapper<UnitRestEntity> unitRestEntityPatchMapper;
    private final UnitEntityMapper unitEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;

    @Autowired
    public UnitFacadeImpl(UnitService unitEntityService,
                          LanguageService languageEntityService,
                          UnitRestEntityMapper unitRestEntityMapper,
                          PatchMapper<UnitRestEntity> unitRestEntityPatchMapper,
                          UnitEntityMapper unitEntityMapper,
                          EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.unitEntityService = unitEntityService;
        this.languageEntityService = languageEntityService;
        this.unitRestEntityMapper = unitRestEntityMapper;
        this.unitRestEntityPatchMapper = unitRestEntityPatchMapper;
        this.unitEntityMapper = unitEntityMapper;
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;

        // Initialize patch validator with validation rules
        // Note: getMandatoryLanguageCodes is passed as a method reference and will be invoked
        // dynamically during each validation, ensuring mandatory languages are always current
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("symbol")),
                new LocalizedFieldValidationRule(Set.of("name"), this::getMandatoryLanguageCodes)
        ));
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

    @Transactional(readOnly = true)
    @Override
    public UnitListRestEntity getUnits(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<UnitEntity> unitsPage = unitEntityService.getUnits(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        PagingInfo pagingInfo = new PagingInfo(unitsPage.getNumber(), unitsPage.getSize(), unitsPage.getTotalElements(), unitsPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<UnitRestEntity> unitRestEntities = unitRestEntityMapper.convertAll(unitsPage.getContent(), context);
        UnitListRestEntity result = new UnitListRestEntity(pagingInfo, sortingInfo, unitRestEntities);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(UnitEntity.class));
        }

        return result;
    }

    @Transactional
    public UnitRestEntity getUnit(String symbol, Set<String> expand) throws NotFoundException, DataMappingException {
        UnitEntity unit = unitEntityService.getUnit(symbol);
        if (unit == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", symbol);
            throw new NotFoundException(MessageKeys.ERROR_UNIT_NOT_FOUND, params);
        }

        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        UnitRestEntity result = unitRestEntityMapper.convert(unit, context);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(UnitEntity.class));
        }

        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(UnitEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public UnitRestEntity patch(String symbol, JsonNode patch) throws NotFoundException, DataMappingException, EntityValidationException {
        // Validate that patch doesn't try to modify the symbol field
        List<Message> patchValidationErrors = patchValidator.validate(patch, symbol);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException("Patch validation failed", errorResponse);
        }

        UnitRestEntity unit = getUnit(symbol, Collections.emptySet());
        if (unit == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", symbol);
            throw new NotFoundException(MessageKeys.ERROR_UNIT_NOT_FOUND, params);
        }
        unit = unitRestEntityPatchMapper.applyPatch(patch, unit);

        // Fetch existing entity to preserve timestamps and update in place
        UnitEntity existingUnit = unitEntityService.getUnit(symbol);
        if (existingUnit == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", symbol);
            throw new NotFoundException(MessageKeys.ERROR_UNIT_NOT_FOUND, params);
        }

        unitEntityMapper.convert(unit, existingUnit, new RestRequestMappingContext<>(symbol));
        UnitEntity saved = unitEntityService.save(existingUnit);
        return unitRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public UnitRestEntity createOrRecreate(String symbol, UnitRestEntity unitRestEntity) throws DataMappingException, EntityValidationException {
        UnitEntity unit = unitEntityService.getUnit(symbol);

        if (unit != null) {
            // Update existing unit

            unitEntityMapper.convert(unitRestEntity, unit, new RestRequestMappingContext<>(symbol));
            UnitEntity saved = unitEntityService.save(unit);
            return unitRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new unit with the symbol from the path
            UnitEntity newUnit = unitEntityMapper.convert(unitRestEntity, new RestRequestMappingContext<>(symbol));

            UnitEntity saved = unitEntityService.save(newUnit);
            return unitRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }

    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class, EntityAlreadyExistsException.class, InvalidParameterException.class})
    public UnitRestEntity create(UnitRestEntity unitRestEntity) throws DataMappingException, EntityAlreadyExistsException, InvalidParameterException, EntityValidationException {
        if (unitRestEntity.getSymbol() == null || unitRestEntity.getSymbol().isEmpty()) {
            throw new InvalidParameterException(MessageKeys.ERROR_VALIDATION_ID_REQUIRED, List.of("symbol"));
        }

        // Check if unit already exists
        UnitEntity existingUnit = unitEntityService.getUnit(unitRestEntity.getSymbol());
        if (existingUnit != null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", unitRestEntity.getSymbol());
            throw new EntityAlreadyExistsException(MessageKeys.ERROR_UNIT_ALREADY_EXISTS, params, List.of("symbol"));
        }
        UnitEntity newUnit = unitEntityMapper.convert(unitRestEntity, new RestRequestMappingContext<>(unitRestEntity.getSymbol()));
        UnitEntity saved = unitEntityService.save(newUnit);
        return unitRestEntityMapper.convert(saved, new RestResponseMappingContext());

    }

    @Transactional
    public void delete(String symbol) {
        UnitEntity unit = unitEntityService.getUnit(symbol);
        if (unit == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", symbol);
            throw new IllegalArgumentException(MessageKeys.ERROR_UNIT_NOT_FOUND);
        }

        unitEntityService.deleteUnit(symbol);
    }

    public void bulkDeleteUnits(List<String> symbols) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();

        for (String symbol : symbols) {
            UnitEntity unit = unitEntityService.getUnit(symbol);
            if (unit != null) {
                try {
                    unitEntityService.deleteUnit(symbol);
                } catch (DataIntegrityViolationException ex) {
                    failedDeletes.add(symbol);
                } catch (ConstraintViolationException ex) {
                    failedDeletes.add(symbol);
                } catch (Exception ex) {
                    // Check for DataIntegrityViolationException or ConstraintViolationException in the cause chain
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof DataIntegrityViolationException || cause instanceof ConstraintViolationException) {
                            failedDeletes.add(symbol);
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
    public UnitListRestEntity createOrUpdateAllUnits(List<UnitRestEntity> unitRestEntities) {
        if (unitRestEntities == null || unitRestEntities.isEmpty()) {
            UnitListRestEntity result = new UnitListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_VALIDATION_EMPTY_REQUEST,
                    "entity", "unit"
            ));
            return result;
        }

        if (unitRestEntities.size() > 100) {
            UnitListRestEntity result = new UnitListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED,
                    "maxItems", "100",
                    "entity", "units"
            ));
            return result;
        }

        List<UnitRestEntity> results = new java.util.ArrayList<>();

        for (UnitRestEntity restEntity : unitRestEntities) {
            try {
                if (restEntity.getSymbol() == null || restEntity.getSymbol().isEmpty()) {
                    UnitRestEntity errorEntity = new UnitRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(
                            Message.MessageType.ERROR,
                            MessageKeys.ERROR_VALIDATION_MANDATORY_FIELD,
                            "field", "symbol",
                            List.of("symbol")
                    ));
                    results.add(errorEntity);
                    continue;
                }

                UnitEntity existingUnit = unitEntityService.getUnit(restEntity.getSymbol());
                if (existingUnit != null) {
                    // Update existing
                    unitEntityMapper.convert(restEntity, existingUnit, new RestRequestMappingContext<>(restEntity.getSymbol()));
                    UnitEntity saved = unitEntityService.save(existingUnit);
                    results.add(unitRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new
                    UnitEntity newUnit = unitEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getSymbol()));
                    UnitEntity saved = unitEntityService.save(newUnit);
                    results.add(unitRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                UnitRestEntity errorEntity = new UnitRestEntity();
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                UnitRestEntity errorEntity = new UnitRestEntity();
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing unit with symbol {}: {}", restEntity.getSymbol(), e.getMessage(), e);
                UnitRestEntity errorEntity = new UnitRestEntity();
                errorEntity.setSymbol(restEntity.getSymbol());
                errorEntity.addMessage(MessageBuilder.create(
                        Message.MessageType.ERROR,
                        MessageKeys.ERROR_PROCESSING,
                        "entity", "unit"
                ));
                results.add(errorEntity);
            }
        }

        return new UnitListRestEntity(null, null, results);
    }
}

