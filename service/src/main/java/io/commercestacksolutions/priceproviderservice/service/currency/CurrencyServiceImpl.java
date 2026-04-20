package io.commercestacksolutions.priceproviderservice.service.currency;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.PermissionFilterBuilder;
import io.commercestacksolutions.commons.query.QueryExpression;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.SpecificationBuilder;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
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
import java.util.Set;

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
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;

    @Autowired
    public CurrencyServiceImpl(
            CurrencyEntityRepository currencyEntityRepository,
            List<ValidationRule<CurrencyEntity>> validationRules,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService) {
        this.currencyEntityRepository = currencyEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(CurrencyEntity.class);
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
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
        validateEntity(currencyEntity);
        updateAuditTimestamps(currencyEntity);

        // Check write permission before saving
        entityAuthorizationService.checkAccess(currencyEntity, getEntityTypeName(), "write",
            currencyEntity.getCurrencyKey() != null ? currencyEntity.getCurrencyKey() : "new");

        return currencyEntityRepository.save(currencyEntity);
    }

    @Transactional
    public void deleteCurrency(String currencyKey) {
        currencyEntityRepository.findById(currencyKey).ifPresent(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "delete", currencyKey);
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

        // Build specification from permission selectors
        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
        Specification<CurrencyEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "Currency", "read");

        // Build specification from user query (if provided)
        Specification<CurrencyEntity> querySpec = null;
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            querySpec = SpecificationBuilder.build(expression);
        }

        // Combine specifications
        Specification<CurrencyEntity> combinedSpec;
        if (permissionSpec != null && querySpec != null) {
            // Both permission filter and query filter present: AND them together
            combinedSpec = permissionSpec.and(querySpec);
        } else if (permissionSpec != null) {
            // Only permission filter
            combinedSpec = permissionSpec;
        } else if (querySpec != null) {
            // Only query filter (user has global permission)
            combinedSpec = querySpec;
        } else {
            // No filters at all (global permission, no query)
            return currencyEntityRepository.findAll(pageRequest);
        }

        return currencyEntityRepository.findAll(combinedSpec, pageRequest);
    }

    public CurrencyEntity getCurrency(String currencyKey) {
        return currencyEntityRepository.findById(currencyKey).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", currencyKey);
            return entity;
        }).orElse(null);
    }

}
