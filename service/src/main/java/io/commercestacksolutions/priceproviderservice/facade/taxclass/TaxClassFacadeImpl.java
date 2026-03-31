package io.commercestacksolutions.priceproviderservice.facade.taxclass;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
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
import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.mapper.TaxClassEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.mapper.TaxClassRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
import io.commercestacksolutions.priceproviderservice.service.taxclass.TaxClassService;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TaxClassFacadeImpl implements TaxClassFacade {

    private static final Logger logger = LoggerFactory.getLogger(TaxClassFacadeImpl.class);

    private final TaxClassService taxClassEntityService;
    private final TaxClassRestEntityMapper taxClassRestEntityMapper;
    private final PatchMapper<TaxClassRestEntity> taxClassRestEntityPatchMapper;
    private final TaxClassEntityMapper taxClassEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;

    @Autowired
    public TaxClassFacadeImpl(TaxClassService taxClassEntityService,
                          TaxClassRestEntityMapper taxClassRestEntityMapper,
                          PatchMapper<TaxClassRestEntity> taxClassRestEntityPatchMapper,
                          TaxClassEntityMapper taxClassEntityMapper,
                          PriceRowEntityRepository priceRowEntityRepository,
                              EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.taxClassEntityService = taxClassEntityService;
        this.taxClassRestEntityMapper = taxClassRestEntityMapper;
        this.taxClassRestEntityPatchMapper = taxClassRestEntityPatchMapper;
        this.taxClassEntityMapper = taxClassEntityMapper;

        // Initialize patch validator with validation rules
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("taxClassId"))
        ));
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;
    }

    @Transactional
    @Override
    public TaxClassListRestEntity getTaxClasses(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> includes, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<TaxClassEntity> taxClassesPage = taxClassEntityService.getTaxClasses(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(includes);
        PagingInfo pagingInfo = new PagingInfo(taxClassesPage.getNumber(), taxClassesPage.getSize(), taxClassesPage.getTotalElements(), taxClassesPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<TaxClassRestEntity> taxClassRestEntities = taxClassRestEntityMapper.convertAll(taxClassesPage.getContent(), context);
        TaxClassListRestEntity result = new TaxClassListRestEntity(pagingInfo, sortingInfo, taxClassRestEntities);

        // Add metadata if requested
        if (includes != null && includes.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(TaxClassEntity.class));
        }

        return result;
    }

    @Transactional
    public TaxClassRestEntity getTaxClass(String taxClassId, Set<String> expand) throws NotFoundException, DataMappingException {
        TaxClassEntity taxClass = taxClassEntityService.getTaxClass(taxClassId);
        if (taxClass == null) {
            Map<String, String> params = new HashMap<>();
            params.put("taxClassId", taxClassId);
            throw new NotFoundException(MessageKeys.ERROR_TAX_CLASS_NOT_FOUND, params);
        }
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        TaxClassRestEntity result = taxClassRestEntityMapper.convert(taxClass, context);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(TaxClassEntity.class));
        }

        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(TaxClassEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public TaxClassRestEntity patch(String taxClassId, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify the taxClassId field
        List<Message> patchValidationErrors = patchValidator.validate(patch, taxClassId);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, errorResponse);
        }

        TaxClassRestEntity taxClass = getTaxClass(taxClassId, Collections.emptySet());
        if (taxClass == null) {
            Map<String, String> params = new HashMap<>();
            params.put("taxClassId", taxClassId);
            throw new NotFoundException(MessageKeys.ERROR_TAX_CLASS_NOT_FOUND, params);
        }
        taxClass = taxClassRestEntityPatchMapper.applyPatch(patch, taxClass);
        
        // Fetch existing entity to preserve timestamps and update in place
        TaxClassEntity existingTaxClass = taxClassEntityService.getTaxClass(taxClassId);
        if (existingTaxClass == null) {
            Map<String, String> params = new HashMap<>();
            params.put("taxClassId", taxClassId);
            throw new NotFoundException(MessageKeys.ERROR_TAX_CLASS_NOT_FOUND, params);
        }
        taxClassEntityMapper.convert(taxClass, existingTaxClass, new RestRequestMappingContext<>(taxClassId));
        TaxClassEntity saved = taxClassEntityService.save(existingTaxClass);
        return taxClassRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public TaxClassRestEntity createOrRecreate(String taxClassId, TaxClassRestEntity taxClassRestEntity) throws DataMappingException, EntityValidationException {
        TaxClassEntity taxClass = taxClassEntityService.getTaxClass(taxClassId);
        if (taxClass != null) {
            // Update existing taxClass
            taxClassEntityMapper.convert(taxClassRestEntity, taxClass, new RestRequestMappingContext<>(taxClassId));
            TaxClassEntity saved = taxClassEntityService.save(taxClass);
            return taxClassRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new taxClass with the taxClassId from the path
            TaxClassEntity newTaxClass = taxClassEntityMapper.convert(taxClassRestEntity, new RestRequestMappingContext<>(taxClassId));
            TaxClassEntity saved = taxClassEntityService.save(newTaxClass);
            return taxClassRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public TaxClassRestEntity create(TaxClassRestEntity taxClassRestEntity) throws DataMappingException, EntityValidationException {
        if (taxClassRestEntity.getTaxClassId() == null || taxClassRestEntity.getTaxClassId().isEmpty()) {
            TaxClassRestEntity restEntity = new TaxClassRestEntity();
            restEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "taxClassId", List.of("taxClassId")));
            return restEntity;
        }

        // Check if taxClass already exists
        TaxClassEntity existingTaxClass = taxClassEntityService.getTaxClass(taxClassRestEntity.getTaxClassId());
        if (existingTaxClass != null) {
            TaxClassRestEntity restEntity = new TaxClassRestEntity();
            Map<String, String> params = new HashMap<>();
            params.put("taxClassId", taxClassRestEntity.getTaxClassId());
            restEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_TAX_CLASS_ALREADY_EXISTS, params, List.of("taxClassId")));
            return restEntity;
        }

        TaxClassEntity newTaxClass = taxClassEntityMapper.convert(taxClassRestEntity, new RestRequestMappingContext<>(taxClassRestEntity.getTaxClassId()));
        TaxClassEntity saved = taxClassEntityService.save(newTaxClass);
        return taxClassRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    public void delete(String taxClassId) throws NotFoundException {
        // Check if there are any price rows referencing this tax class
        // Note: We'll need to update this once we change PriceRow to use TaxClassEntity
        TaxClassEntity taxClass = taxClassEntityService.getTaxClass(taxClassId);
        if (taxClass == null) {
            Map<String, String> params = new HashMap<>();
            params.put("taxClassId", taxClassId);
            throw new NotFoundException(MessageKeys.ERROR_TAX_CLASS_NOT_FOUND, params);
        }

        // For now, just delete - we'll add reference checking when we update PriceRow
        taxClassEntityService.deleteTaxClass(taxClassId);
    }

    public void bulkDeleteTaxClasses(List<String> taxClassIds) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();

        for (String taxClassId : taxClassIds) {
            TaxClassEntity taxClass = taxClassEntityService.getTaxClass(taxClassId);
            if (taxClass != null) {
                try {
                    taxClassEntityService.deleteTaxClass(taxClassId);
                } catch (DataIntegrityViolationException ex) {
                    failedDeletes.add(taxClassId);
                } catch (ConstraintViolationException ex) {
                    failedDeletes.add(taxClassId);
                } catch (Exception ex) {
                    // Check for DataIntegrityViolationException or ConstraintViolationException in the cause chain
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof DataIntegrityViolationException || cause instanceof ConstraintViolationException) {
                            failedDeletes.add(taxClassId);
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
    public TaxClassListRestEntity createOrUpdateAllTaxClasses(List<TaxClassRestEntity> taxClassRestEntities) {
        if (taxClassRestEntities == null || taxClassRestEntities.isEmpty()) {
            TaxClassListRestEntity result = new TaxClassListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_VALIDATION_EMPTY_REQUEST,
                "entity", "taxClass"
            ));
            return result;
        }

        if (taxClassRestEntities.size() > 100) {
            TaxClassListRestEntity result = new TaxClassListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED,
                "maxItems", "100",
                "entity", "taxClasses"
            ));
            return result;
        }

        List<TaxClassRestEntity> results = new java.util.ArrayList<>();

        for (TaxClassRestEntity restEntity : taxClassRestEntities) {
            try {
                if (restEntity.getTaxClassId() == null || restEntity.getTaxClassId().isEmpty()) {
                    TaxClassRestEntity errorEntity = new TaxClassRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(
                        Message.MessageType.ERROR,
                        MessageKeys.ERROR_VALIDATION_MANDATORY_FIELD,
                        "field", "taxClassId",
                        List.of("taxClassId")
                    ));
                    results.add(errorEntity);
                    continue;
                }

                TaxClassEntity existingTaxClass = taxClassEntityService.getTaxClass(restEntity.getTaxClassId());
                if (existingTaxClass != null) {
                    // Update existing
                    taxClassEntityMapper.convert(restEntity, existingTaxClass, new RestRequestMappingContext<>(restEntity.getTaxClassId()));
                    TaxClassEntity saved = taxClassEntityService.save(existingTaxClass);
                    results.add(taxClassRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new
                    TaxClassEntity newTaxClass = taxClassEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getTaxClassId()));
                    TaxClassEntity saved = taxClassEntityService.save(newTaxClass);
                    results.add(taxClassRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                TaxClassRestEntity errorEntity = new TaxClassRestEntity();
                errorEntity.setTaxClassId(restEntity.getTaxClassId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                TaxClassRestEntity errorEntity = new TaxClassRestEntity();
                errorEntity.setTaxClassId(restEntity.getTaxClassId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing taxClass with id {}: {}", restEntity.getTaxClassId(), e.getMessage(), e);
                TaxClassRestEntity errorEntity = new TaxClassRestEntity();
                errorEntity.setTaxClassId(restEntity.getTaxClassId());
                errorEntity.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_PROCESSING,
                    "entity", "taxClass"
                ));
                results.add(errorEntity);
            }
        }

        return new TaxClassListRestEntity(null, null, results);
    }
}
