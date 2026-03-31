package io.commercestacksolutions.priceproviderservice.facade.country;

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
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.facade.country.mapper.CountryEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.country.mapper.CountryRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.country.restentity.CountryListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.country.restentity.CountryRestEntity;
import io.commercestacksolutions.priceproviderservice.service.country.CountryService;
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
public class CountryFacadeImpl implements CountryFacade {

    private static final Logger logger = LoggerFactory.getLogger(CountryFacadeImpl.class);

    private final CountryService countryService;
    private final CountryRestEntityMapper countryRestEntityMapper;
    private final PatchMapper<CountryRestEntity> countryRestEntityPatchMapper;
    private final CountryEntityMapper countryEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;

    @Autowired
    public CountryFacadeImpl(CountryService countryService,
                             CountryRestEntityMapper countryRestEntityMapper,
                             PatchMapper<CountryRestEntity> countryRestEntityPatchMapper,
                             CountryEntityMapper countryEntityMapper,
                              EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.countryService = countryService;
        this.countryRestEntityMapper = countryRestEntityMapper;
        this.countryRestEntityPatchMapper = countryRestEntityPatchMapper;
        this.countryEntityMapper = countryEntityMapper;

        // Initialize patch validator with immutable field rule for isoKey
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("isoKey"))
        ));
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;
    }

    @Transactional
    @Override
    public CountryListRestEntity getCountries(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> includes, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<CountryEntity> countriesPage = countryService.getCountries(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(includes);
        PagingInfo pagingInfo = new PagingInfo(countriesPage.getNumber(), countriesPage.getSize(), countriesPage.getTotalElements(), countriesPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<CountryRestEntity> countryRestEntities = countryRestEntityMapper.convertAll(countriesPage.getContent(), context);
        CountryListRestEntity result = new CountryListRestEntity(pagingInfo, sortingInfo, countryRestEntities);

        // Add metadata if requested
        if (includes != null && includes.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(CountryEntity.class));
        }

        return result;
    }

    @Transactional
    @Override
    public CountryRestEntity getCountry(String isoKey, Set<String> expand) throws NotFoundException, DataMappingException {
        CountryEntity country = countryService.getCountry(isoKey);
        if (country == null) {
            Map<String, String> params = new HashMap<>();
            params.put("isoKey", isoKey);
            throw new NotFoundException(MessageKeys.ERROR_COUNTRY_NOT_FOUND, params);
        }
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        CountryRestEntity result = countryRestEntityMapper.convert(country, context);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(CountryEntity.class));
        }

        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(CountryEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public CountryRestEntity patch(String isoKey, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify the isoKey field
        List<Message> patchValidationErrors = patchValidator.validate(patch, isoKey);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, errorResponse);
        }

        CountryRestEntity country = getCountry(isoKey, Collections.emptySet());
        if (country == null) {
            Map<String, String> params = new HashMap<>();
            params.put("isoKey", isoKey);
            throw new NotFoundException(MessageKeys.ERROR_COUNTRY_NOT_FOUND, params);
        }
        country = countryRestEntityPatchMapper.applyPatch(patch, country);

        // Fetch existing entity to preserve timestamps and update in place
        CountryEntity existingCountry = countryService.getCountry(isoKey);
        if (existingCountry == null) {
            Map<String, String> params = new HashMap<>();
            params.put("isoKey", isoKey);
            throw new NotFoundException(MessageKeys.ERROR_COUNTRY_NOT_FOUND, params);
        }
        countryEntityMapper.convert(country, existingCountry, new RestRequestMappingContext<>(isoKey));
        CountryEntity saved = countryService.save(existingCountry);
        return countryRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public CountryRestEntity createOrRecreate(String isoKey, CountryRestEntity countryRestEntity) throws DataMappingException, EntityValidationException {
        CountryEntity country = countryService.getCountry(isoKey);
        if (country != null) {
            // Update existing country
            countryEntityMapper.convert(countryRestEntity, country, new RestRequestMappingContext<>(isoKey));
            CountryEntity saved = countryService.save(country);
            return countryRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new country with isoKey from path
            CountryEntity newCountry = countryEntityMapper.convert(countryRestEntity, new RestRequestMappingContext<>(isoKey));
            CountryEntity saved = countryService.save(newCountry);
            return countryRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public CountryRestEntity create(CountryRestEntity countryRestEntity) throws DataMappingException, EntityValidationException {
        if (countryRestEntity.getIsoKey() == null || countryRestEntity.getIsoKey().isEmpty()) {
            CountryRestEntity restEntity = new CountryRestEntity();
            restEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "isoKey", List.of("isoKey")));
            return restEntity;
        }

        // Check if country already exists
        CountryEntity existingCountry = countryService.getCountry(countryRestEntity.getIsoKey());
        if (existingCountry != null) {
            CountryRestEntity restEntity = new CountryRestEntity();
            Map<String, String> params = new HashMap<>();
            params.put("isoKey", countryRestEntity.getIsoKey());
            restEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_COUNTRY_ALREADY_EXISTS, params, List.of("isoKey")));
            return restEntity;
        }

        CountryEntity newCountry = countryEntityMapper.convert(countryRestEntity, new RestRequestMappingContext<>(countryRestEntity.getIsoKey()));
        CountryEntity saved = countryService.save(newCountry);
        return countryRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    @Override
    public void delete(String isoKey) throws NotFoundException {
        CountryEntity country = countryService.getCountry(isoKey);
        if (country == null) {
            Map<String, String> params = new HashMap<>();
            params.put("isoKey", isoKey);
            throw new NotFoundException(MessageKeys.ERROR_COUNTRY_NOT_FOUND, params);
        }
        countryService.deleteCountry(isoKey);
    }

    @Override
    public void bulkDeleteCountries(List<String> isoKeys) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();

        for (String isoKey : isoKeys) {
            CountryEntity country = countryService.getCountry(isoKey);
            if (country != null) {
                try {
                    countryService.deleteCountry(isoKey);
                } catch (DataIntegrityViolationException ex) {
                    failedDeletes.add(isoKey);
                } catch (ConstraintViolationException ex) {
                    failedDeletes.add(isoKey);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof DataIntegrityViolationException || cause instanceof ConstraintViolationException) {
                            failedDeletes.add(isoKey);
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
            params.put("ids", String.join(", ", failedDeletes));
            throw new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
        }
    }

    @Transactional
    @Override
    public CountryListRestEntity createOrUpdateAllCountries(List<CountryRestEntity> countryRestEntities) {
        if (countryRestEntities == null || countryRestEntities.isEmpty()) {
            CountryListRestEntity result = new CountryListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_VALIDATION_EMPTY_REQUEST,
                    "entity", "country"
            ));
            return result;
        }

        if (countryRestEntities.size() > 100) {
            CountryListRestEntity result = new CountryListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED,
                    "maxItems", "100",
                    "entity", "countries"
            ));
            return result;
        }

        List<CountryRestEntity> results = new java.util.ArrayList<>();

        for (CountryRestEntity restEntity : countryRestEntities) {
            try {
                if (restEntity.getIsoKey() == null || restEntity.getIsoKey().isEmpty()) {
                    CountryRestEntity errorEntity = new CountryRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(
                            Message.MessageType.ERROR,
                            MessageKeys.ERROR_VALIDATION_MANDATORY_FIELD,
                            "field", "isoKey",
                            List.of("isoKey")
                    ));
                    results.add(errorEntity);
                    continue;
                }

                CountryEntity existingCountry = countryService.getCountry(restEntity.getIsoKey());
                if (existingCountry != null) {
                    // Update existing
                    countryEntityMapper.convert(restEntity, existingCountry, new RestRequestMappingContext<>(restEntity.getIsoKey()));
                    CountryEntity saved = countryService.save(existingCountry);
                    results.add(countryRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new
                    CountryEntity newCountry = countryEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getIsoKey()));
                    CountryEntity saved = countryService.save(newCountry);
                    results.add(countryRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                CountryRestEntity errorEntity = new CountryRestEntity();
                errorEntity.setIsoKey(restEntity.getIsoKey());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                CountryRestEntity errorEntity = new CountryRestEntity();
                errorEntity.setIsoKey(restEntity.getIsoKey());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing country with isoKey {}: {}", restEntity.getIsoKey(), e.getMessage(), e);
                CountryRestEntity errorEntity = new CountryRestEntity();
                errorEntity.setIsoKey(restEntity.getIsoKey());
                errorEntity.addMessage(MessageBuilder.create(
                        Message.MessageType.ERROR,
                        MessageKeys.ERROR_PROCESSING,
                        "entity", "country"
                ));
                results.add(errorEntity);
            }
        }

        return new CountryListRestEntity(null, null, results);
    }
}
