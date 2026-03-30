package de.ebusyness.priceproviderservice.facade.pricerow;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.PatchMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.mapper.validation.PatchValidator;
import de.ebusyness.commons.mapper.validation.rules.ImmutableFieldsRule;
import de.ebusyness.commons.mapper.validation.rules.MandatoryFieldsRule;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.*;
import de.ebusyness.commons.dataaccess.meta.EntityMetaInfoRegistry;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.facade.pricerow.mapper.PriceRowEntityMapper;
import de.ebusyness.priceproviderservice.facade.pricerow.mapper.PriceRowRestEntityMapper;
import de.ebusyness.priceproviderservice.facade.pricerow.restentity.PriceRowListRestEntity;
import de.ebusyness.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import de.ebusyness.priceproviderservice.service.pricerow.PriceRowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    public PriceRowRestEntity getPriceRow(Long id, Set<String> expand) throws DataMappingException, NotFoundException {
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
            params.put("id", String.valueOf(id));
            throw new NotFoundException(MessageKeys.ERROR_PRICE_ROW_NOT_FOUND, params);
        }
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(PriceRowEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    public PriceRowRestEntity createOrRecreate(Long id, PriceRowRestEntity priceRowRestEntity) throws DataMappingException, EntityValidationException {
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
    public PriceRowRestEntity patch(Long id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify protected fields
        List<Message> patchValidationErrors = getPatchValidator().validate(patch, id);
        if (!patchValidationErrors.isEmpty()) {
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, new ErrorResponse(patchValidationErrors));
        }

        PriceRowRestEntity priceRowRestEntity = getPriceRow(id, Collections.emptySet());
        priceRowRestEntity = priceRowRestEntityPatchMapper.applyPatch(patch, priceRowRestEntity);
        
        // Fetch existing entity to preserve timestamps and update in place
        Optional<PriceRowEntity> existingPriceRowOpt = priceRowService.findById(id);
        if (existingPriceRowOpt.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("id", String.valueOf(id));
            throw new NotFoundException(MessageKeys.ERROR_PRICE_ROW_NOT_FOUND, params, List.of("id"));
        }
        PriceRowEntity existingPriceRow = existingPriceRowOpt.get();
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
    public void delete(Long id) throws NotFoundException {
        Optional<PriceRowEntity> priceRowEntityOptional = priceRowService.findById(id);
        if (priceRowEntityOptional.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("id", String.valueOf(id));
            throw new NotFoundException(MessageKeys.ERROR_PRICE_ROW_NOT_FOUND, params, List.of("id"));
        }
        priceRowService.deleteById(id);
    }

    public void bulkDeletePriceRows(List<Long> ids) throws de.ebusyness.commons.exception.DataIntegrityException {
        List<Long> failedDeletes = new java.util.ArrayList<>();
        
        for (Long id : ids) {
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
            params.put("ids", failedDeletes.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", ")));
            throw new de.ebusyness.commons.exception.DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
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
                    Optional<PriceRowEntity> existingByFields = priceRowService.findByMatchingFields(
                        restEntity.getPricedResourceId(),
                        restEntity.getMinQuantity(),
                        restEntity.getUnitRef(),
                        restEntity.getCurrencyRef(),
                        restEntity.getTaxClassRef(),
                        restEntity.isTaxIncluded(),
                        restEntity.getPriceType(),
                        restEntity.getValidFrom(),
                        restEntity.getValidTo(),
                        restEntity.getGroupRefs()
                    );
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

