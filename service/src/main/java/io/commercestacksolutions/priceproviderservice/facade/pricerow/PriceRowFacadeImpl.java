package io.commercestacksolutions.priceproviderservice.facade.pricerow;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.PatchMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.mapper.validation.PatchValidator;
import io.commercestacksolutions.commons.mapper.validation.rules.ImmutableFieldsRule;
import io.commercestacksolutions.commons.mapper.validation.rules.MandatoryFieldsRule;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.*;
import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceType;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.mapper.PriceRowEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.mapper.PriceRowRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import io.commercestacksolutions.priceproviderservice.service.pricerow.PriceRowService;
import io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching.PriceRowMatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PriceRowFacadeImpl implements PriceRowFacade {

    private static final Logger logger = LoggerFactory.getLogger(PriceRowFacadeImpl.class);

    public static final int CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS = 100;

    private final PriceRowEntityMapper priceRowEntityMapper;
    private final PriceRowRestEntityMapper priceRowRestEntityMapper;
    private final PriceRowService priceRowService;
    private final PatchMapper<PriceRowRestEntity> priceRowRestEntityPatchMapper;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;

    // Lazily initialized after @PostConstruct of MetaInfoRegistryConfig has run
    private volatile PatchValidator patchValidator;

    @Autowired
    public PriceRowFacadeImpl(PriceRowEntityMapper priceRowEntityConverter, PriceRowRestEntityMapper priceRowRestEntityMapper, PriceRowService priceRowService, PatchMapper<PriceRowRestEntity> priceRowRestEntityPatchMapper, EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.priceRowEntityMapper = priceRowEntityConverter;
        this.priceRowRestEntityMapper = priceRowRestEntityMapper;
        this.priceRowService = priceRowService;
        this.priceRowRestEntityPatchMapper = priceRowRestEntityPatchMapper;
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;
    }

    /**
     * Lazily builds the {@link PatchValidator} on first use.
     * By the time any request arrives all {@code @PostConstruct} lifecycle callbacks
     * have already run (including {@code MetaInfoRegistryConfig.registerEntityMetaInfos()}),
     * so the registry is guaranteed to be populated here.
     */
    private PatchValidator getPatchValidator() {
        if (patchValidator == null) {
            MetaInfo meta = entityMetaInfoRegistry.getMetaInfo(PriceRowEntity.class);
            Set<String> mandatoryFields = meta != null && meta.getMandatoryFields() != null
                    ? new HashSet<>(meta.getMandatoryFields()) : Collections.emptySet();
            patchValidator = new PatchValidator(List.of(
                    new ImmutableFieldsRule(Set.of("id")),
                    new MandatoryFieldsRule(mandatoryFields)
            ));
        }
        return patchValidator;
    }

    @Transactional
    @Override
    public PriceRowListRestEntity getPriceRows(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<PriceRowEntity> priceRowPage = priceRowService.findAll(page, pageSize, sortBy, sortDirection, query);
        Collection<PriceRowRestEntity> priceRowRestEntities = priceRowRestEntityMapper.convertAll(priceRowPage.getContent(), getRestBasicsMappingContext(expand));
        PagingInfo pagingInfo = getPagingInfo(priceRowPage);
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        PriceRowListRestEntity result = new PriceRowListRestEntity(pagingInfo, sortingInfo, priceRowRestEntities);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(PriceRowEntity.class));
        }

        return result;
    }

    @Transactional
    public PriceRowRestEntity getPriceRow(String id, Set<String> expand) throws DataMappingException, NotFoundException {
        Optional<PriceRowEntity> priceRowEntityOptional = priceRowService.findById(id);
        if (priceRowEntityOptional.isPresent()) {
            PriceRowEntity priceRowEntity = priceRowEntityOptional.get();

            RestResponseMappingContext mappingContext = getRestBasicsMappingContext(expand);
            PriceRowRestEntity priceRowRestEntity = priceRowRestEntityMapper.convert(priceRowEntity, mappingContext);

            // Add metadata if requested
            if (expand != null && expand.contains("$meta")) {
                priceRowRestEntity.setMeta(entityMetaInfoRegistry.getMetaInfo(PriceRowEntity.class));
            }

            return priceRowRestEntity;
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_PRICE_ROW_NOT_FOUND, params);
        }
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(PriceRowEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public PriceRowRestEntity createOrRecreate(String id, PriceRowRestEntity priceRowRestEntity) throws DataMappingException, EntityValidationException {
        Optional<PriceRowEntity> priceRowEntityOptional = priceRowService.findById(id);
        if (priceRowEntityOptional.isPresent()) {
            // Update existing price row
            PriceRowEntity priceRowEntity = priceRowEntityOptional.get();

            priceRowEntityMapper.convert(priceRowRestEntity, priceRowEntity, new RestRequestMappingContext<>(id));
            PriceRowEntity saved = priceRowService.save(priceRowEntity);
            return priceRowRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new price row with the id from the path
            PriceRowEntity newPriceRow = priceRowEntityMapper.convert(priceRowRestEntity, new RestRequestMappingContext<>(id));

            PriceRowEntity saved = priceRowService.save(newPriceRow);
            return priceRowRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public PriceRowRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify protected fields
        List<Message> patchValidationErrors = getPatchValidator().validate(patch, id);
        if (!patchValidationErrors.isEmpty()) {
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, new ErrorResponse(patchValidationErrors));
        }

        // Fetch existing entity
        Optional<PriceRowEntity> existingPriceRowOpt = priceRowService.findById(id);
        if (existingPriceRowOpt.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_PRICE_ROW_NOT_FOUND, params, List.of("id"));
        }
        PriceRowEntity existingPriceRow = existingPriceRowOpt.get();

        // Apply patch
        PriceRowRestEntity priceRowRestEntity = priceRowRestEntityMapper.convert(existingPriceRow, new RestResponseMappingContext());
        priceRowRestEntity = priceRowRestEntityPatchMapper.applyPatch(patch, priceRowRestEntity);

        priceRowEntityMapper.convert(priceRowRestEntity, existingPriceRow, new RestRequestMappingContext<>(id));
        PriceRowEntity saved = priceRowService.save(existingPriceRow);
        return priceRowRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {DataMappingException.class})
    public PriceRowRestEntity create(PriceRowRestEntity priceRowRestEntity) throws DataMappingException, InvalidParameterException {
        PriceRowEntity newPriceRow = priceRowEntityMapper.convert(priceRowRestEntity, new RestRequestMappingContext<>(null));

        try {
            PriceRowEntity saved = priceRowService.save(newPriceRow);
            return priceRowRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } catch (EntityValidationException e) {
            PriceRowRestEntity restEntity = new PriceRowRestEntity();
            e.getMessages().forEach(restEntity::addMessage);
            return restEntity;
        }
    }

    @Transactional
    public void delete(String id) throws NotFoundException {
        Optional<PriceRowEntity> priceRowEntityOptional = priceRowService.findById(id);
        if (priceRowEntityOptional.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_PRICE_ROW_NOT_FOUND, params, List.of("id"));
        }

        priceRowService.deleteById(id);
    }

    public void bulkDeletePriceRows(List<String> ids) throws io.commercestacksolutions.commons.exception.DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();
        
        for (String id : ids) {
            if (priceRowService.findById(id).isPresent()) {
                try {
                    priceRowService.deleteById(id);
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                    failedDeletes.add(id);
                } catch (Exception ex) {
                    // Check for DataIntegrityViolationException in the cause chain
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof org.springframework.dao.DataIntegrityViolationException) {
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
            throw new io.commercestacksolutions.commons.exception.DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
        }
    }

    @Transactional
    public PriceRowListRestEntity createOrUpdateAllPriceRows(List<PriceRowRestEntity> priceRowRestEntities) {
        if (priceRowRestEntities == null || priceRowRestEntities.isEmpty()) {
            PriceRowListRestEntity result = new PriceRowListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_VALIDATION_EMPTY_REQUEST,
                "entity", "priceRow"
            ));
            return result;
        }

        if (priceRowRestEntities.size() > CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS) {
            PriceRowListRestEntity result = new PriceRowListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                Message.MessageType.ERROR,
                MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED,
                "maxItems", String.valueOf(CREATE_OR_UPDATE_MAX_ALLOWED_ITEMS),
                "entity", "priceRows"
            ));
            return result;
        }

        List<PriceRowRestEntity> results = new java.util.ArrayList<>();

        for (PriceRowRestEntity restEntity : priceRowRestEntities) {
            try {
                PriceRowEntity priceRowEntity = null;
                
                if (restEntity.getId() != null) {
                    // ID is provided - check if it exists
                    Optional<PriceRowEntity> existingById = priceRowService.findById(restEntity.getId());
                    if (existingById.isPresent()) {
                        priceRowEntity = existingById.get();
                    }
                } else {
                    // No ID provided - try to find by matching fields
                    PriceRowMatchingContext context = buildPriceRowMatchingContext(restEntity);

                    Optional<PriceRowEntity> existingByFields = priceRowService.findByMatchingFields(context);
                    if (existingByFields.isPresent()) {
                        priceRowEntity = existingByFields.get();
                    }
                }
                
                if (priceRowEntity != null) {
                    // Update existing price row
                    priceRowEntityMapper.convert(restEntity, priceRowEntity, new RestRequestMappingContext<>(priceRowEntity.getId()));
                    PriceRowEntity saved = priceRowService.save(priceRowEntity);
                    results.add(priceRowRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new price row
                    PriceRowEntity newPriceRow = priceRowEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getId()));
                    PriceRowEntity saved = priceRowService.save(newPriceRow);
                    results.add(priceRowRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                PriceRowRestEntity errorEntity = new PriceRowRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                PriceRowRestEntity errorEntity = new PriceRowRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing priceRow with id {}: {}", restEntity.getId(), e.getMessage(), e);
                PriceRowRestEntity errorEntity = new PriceRowRestEntity();
                errorEntity.setId(restEntity.getId());
                errorEntity.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_PROCESSING,
                    "entity", "priceRow"
                ));
                results.add(errorEntity);
            }
        }

        return new PriceRowListRestEntity(null, null, results);
    }

    @NonNull
    private PriceRowMatchingContext buildPriceRowMatchingContext(PriceRowRestEntity restEntity) {
        PriceRowMatchingContext context = new PriceRowMatchingContext();
        context.setPricedResourceId(restEntity.getPricedResourceId());
        context.setMinQuantity(restEntity.getMinQuantity());
        context.setUnitRef(restEntity.getUnitRef());
        context.setCurrencyRef(restEntity.getCurrencyRef());
        context.setTaxClassRef(restEntity.getTaxClassRef());
        context.setTaxIncluded(restEntity.isTaxIncluded());
        context.setPriceType(restEntity.getPriceType() != null ? new PriceType(restEntity.getPriceType()) : null);
        context.setValidFrom(restEntity.getValidFrom());
        context.setValidTo(restEntity.getValidTo());
        context.setGroupRefs(restEntity.getGroupRefs());
        return context;
    }

    private static PagingInfo getPagingInfo(Page<PriceRowEntity> priceRowPage) {
        return new PagingInfo(
                priceRowPage.getNumber(),
                priceRowPage.getSize(),
                priceRowPage.getTotalElements(),
                priceRowPage.getTotalPages()
        );
    }

    private static RestResponseMappingContext getRestBasicsMappingContext(Set<String> expand) {
        RestResponseMappingContext mappingContext = new RestResponseMappingContext();
        mappingContext.addExpandPaths(expand);
        return mappingContext;
    }

}

