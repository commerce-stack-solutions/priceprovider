package io.commercestacksolutions.priceproviderservice.service.language;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.SpecificationCombiner;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.LanguageEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of LanguageService interface.
 * This class provides concrete implementation for language management operations.
 */
@Service
public class LanguageServiceImpl implements LanguageService {

    private static final Logger logger = LoggerFactory.getLogger(LanguageServiceImpl.class);

    private final LanguageEntityRepository languageEntityRepository;
    private final EntityValidator<LanguageEntity> entityValidator;
    private final QueryParser queryParser;
    private final SpecificationCombiner specificationCombiner;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;
    private final EntityManager entityManager;

    @Autowired
    public LanguageServiceImpl(
            LanguageEntityRepository languageEntityRepository,
            List<ValidationRule<LanguageEntity>> validationRules,
            SpecificationCombiner specificationCombiner,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService,
            EntityManager entityManager) {
        this.languageEntityRepository = languageEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(LanguageEntity.class);
        this.specificationCombiner = specificationCombiner;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
        this.entityManager = entityManager;
    }

    @Override
    public Class<LanguageEntity> getTargetClass() {
        return LanguageEntity.class;
    }

    @Override
    public EntityValidator<LanguageEntity> getEntityValidator() {
        return entityValidator;
    }

    public LanguageEntity save(LanguageEntity languageEntity) throws EntityValidationException {
        // Fetch and detach existing entity for permission check
        // Note: This will clear the persistence context, detaching languageEntity
        LanguageEntity existingEntity = fetchAndDetachExistingEntity(
            languageEntity.getIsoKey(), languageEntityRepository, entityManager);

        // Re-attach the incoming entity to the persistence context
        // This is necessary because fetchAndDetachExistingEntity clears the context
        if (languageEntity.getIsoKey() != null) {
            languageEntity = entityManager.merge(languageEntity);
        }

        validateEntity(languageEntity);
        updateAuditTimestamps(languageEntity);

        // Check write permission on both before (existing) and after (new) states
        entityAuthorizationService.checkAccessBeforeAndAfter(
            existingEntity,
            languageEntity,
            getEntityTypeName(),
            "write",
            languageEntity.getIsoKey() != null ? languageEntity.getIsoKey() : "new"
        );

        return languageEntityRepository.save(languageEntity);
    }

    public List<LanguageEntity> getAllLanguages() {
        return languageEntityRepository.findAll();
    }

    public List<LanguageEntity> getActiveLanguages() {
        return languageEntityRepository.findByActive(true);
    }

    public List<LanguageEntity> getMandatoryLanguages() {
        return languageEntityRepository.findByMandatory(true);
    }

    public Optional<LanguageEntity> getLanguageByIsoKey(String isoKey) {
        return languageEntityRepository.findById(isoKey);
    }

    public LanguageEntity updateLanguage(LanguageEntity updatedLanguage) throws EntityValidationException {
        return save(updatedLanguage);
    }

    public void deleteLanguage(String isoKey) {
        languageEntityRepository.findById(isoKey).ifPresent(entity -> {
            // Check delete permission on the existing entity (before deletion)
            entityAuthorizationService.checkAccessBeforeAndAfter(
                entity,
                null,  // No "after" state for delete
                getEntityTypeName(),
                "delete",
                isoKey
            );
            languageEntityRepository.deleteById(isoKey);
        });
    }

    public LanguageEntity findByIsoKey(String isoKey) {
        return languageEntityRepository.findByIsoKey(isoKey);
    }

    // Business logic methods from LanguageService
    @Override
    public Page<LanguageEntity> getLanguages(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
        PageRequest pageRequest;
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            List<Sort.Order> orders = new ArrayList<>();
            for (String field : sortBy) {
                orders.add(new Sort.Order(direction, field));
            }
            pageRequest = PageRequest.of(page, pageSize, Sort.by(orders));
        } else {
            pageRequest = PageRequest.of(page, pageSize);
        }

        // Combine permission-based and query-based filtering
        Specification<LanguageEntity> combinedSpec = specificationCombiner.combine(
                authorizationContext.getCurrentPermissions(), "Language", "read", query, queryParser);

        if (combinedSpec != null) {
            return languageEntityRepository.findAll(combinedSpec, pageRequest);
        } else {
            // No filters at all (global permission, no query)
            return languageEntityRepository.findAll(pageRequest);
        }
    }

    public LanguageEntity getLanguage(String isoKey) {
        return languageEntityRepository.findById(isoKey).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", isoKey);
            return entity;
        }).orElse(null);
    }
}
