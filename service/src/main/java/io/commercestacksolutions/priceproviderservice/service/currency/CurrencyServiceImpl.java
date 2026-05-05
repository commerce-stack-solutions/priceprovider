package io.commercestacksolutions.priceproviderservice.service.currency;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.SpecificationCombiner;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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

/**
 * Implementation of CurrencyService interface.
 * This class provides concrete implementation for currency management operations.
 */
@Service
public class CurrencyServiceImpl implements CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyServiceImpl.class);

    private final CurrencyEntityRepository currencyEntityRepository;
    private final EntityValidator<CurrencyEntity> entityValidator;
    private final QueryParser queryParser;
    private final SpecificationCombiner specificationCombiner;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;
    private final EntityManager entityManager;

    @Autowired
    public CurrencyServiceImpl(
            CurrencyEntityRepository currencyEntityRepository,
            List<ValidationRule<CurrencyEntity>> validationRules,
            SpecificationCombiner specificationCombiner,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService,
            EntityManager entityManager) {
        this.currencyEntityRepository = currencyEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(CurrencyEntity.class);
        this.specificationCombiner = specificationCombiner;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
        this.entityManager = entityManager;
    }

    @Override
    public Class<CurrencyEntity> getTargetClass() {
        return CurrencyEntity.class;
    }

    @Override
    public EntityValidator<CurrencyEntity> getEntityValidator() {
        return entityValidator;
    }

    public CurrencyEntity save(CurrencyEntity currencyEntity) throws EntityValidationException {
        // Fetch and detach existing entity for permission check
        // Note: This will clear the persistence context, detaching currencyEntity
        CurrencyEntity existingEntity = fetchAndDetachExistingEntity(
            currencyEntity.getCurrencyKey(), currencyEntityRepository, entityManager);

        // Re-attach the incoming entity to the persistence context
        // This is necessary because fetchAndDetachExistingEntity clears the context
        if (currencyEntity.getCurrencyKey() != null) {
            currencyEntity = entityManager.merge(currencyEntity);
        }

        validateEntity(currencyEntity);
        updateAuditTimestamps(currencyEntity);

        // Check write permission on both before (existing) and after (new) states
        entityAuthorizationService.checkAccessBeforeAndAfter(
            existingEntity,
            currencyEntity,
            getEntityTypeName(),
            "write",
            currencyEntity.getCurrencyKey() != null ? currencyEntity.getCurrencyKey() : "new"
        );

        return currencyEntityRepository.save(currencyEntity);
    }

    @Transactional
    public void deleteCurrency(String currencyKey) {
        currencyEntityRepository.findById(currencyKey).ifPresent(entity -> {
            // Check delete permission on the existing entity (before deletion)
            entityAuthorizationService.checkAccessBeforeAndAfter(
                entity,
                null,  // No "after" state for delete
                getEntityTypeName(),
                "delete",
                currencyKey
            );
            currencyEntityRepository.deleteById(currencyKey);
        });
    }


    @Override
    public Page<CurrencyEntity> getCurrencies(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
        Specification<CurrencyEntity> combinedSpec = specificationCombiner.combine(
                authorizationContext.getCurrentPermissions(), "Currency", "read", query, queryParser);

        if (combinedSpec != null) {
            return currencyEntityRepository.findAll(combinedSpec, pageRequest);
        } else {
            // No filters at all (global permission, no query)
            return currencyEntityRepository.findAll(pageRequest);
        }
    }

    public CurrencyEntity getCurrency(String currencyKey) {
        return currencyEntityRepository.findById(currencyKey).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", currencyKey);
            return entity;
        }).orElse(null);
    }

}
