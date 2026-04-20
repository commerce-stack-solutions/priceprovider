package io.commercestacksolutions.priceproviderservice.service.country;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.PermissionFilterBuilder;
import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.CountryEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
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
 * Implementation of CountryService interface.
 * This class provides concrete implementation for country management operations.
 */
@Service
public class CountryServiceImpl implements CountryService {

    private static final Logger logger = LoggerFactory.getLogger(CountryServiceImpl.class);

    private final CountryEntityRepository countryEntityRepository;
    private final EntityValidator<CountryEntity> entityValidator;
    private final QueryParser queryParser;
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;

    @Autowired
    public CountryServiceImpl(
            CountryEntityRepository countryEntityRepository,
            List<ValidationRule<CountryEntity>> validationRules,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService) {
        this.countryEntityRepository = countryEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        // Create a QueryParser configured with the entity's field types so parser can validate types
        this.queryParser = new QueryParser(QueryReflectionUtil.buildFieldTypeMap(CountryEntity.class));
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
    }

    @Override
    public Class<CountryEntity> getTargetClass() {
        return CountryEntity.class;
    }

    @Override
    public EntityValidator<CountryEntity> getEntityValidator() {
        return entityValidator;
    }

    @Override
    public CountryEntity save(CountryEntity countryEntity) throws EntityValidationException {
        validateEntity(countryEntity);
        updateAuditTimestamps(countryEntity);

        // Check write permission before saving
        entityAuthorizationService.checkAccess(countryEntity, getEntityTypeName(), "write",
            countryEntity.getIsoKey() != null ? countryEntity.getIsoKey() : "new");

        return countryEntityRepository.save(countryEntity);
    }

    @Override
    public void deleteCountry(String isoKey) {
        countryEntityRepository.findById(isoKey).ifPresent(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "delete", isoKey);
            countryEntityRepository.deleteById(isoKey);
        });
    }

    @Override
    public Page<CountryEntity> getCountries(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
        Specification<CountryEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "Country", "read");

        // Build specification from user query (if provided)
        Specification<CountryEntity> querySpec = null;
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            querySpec = SpecificationBuilder.build(expression);
        }

        // Combine specifications
        Specification<CountryEntity> combinedSpec;
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
            return countryEntityRepository.findAll(pageRequest);
        }

        return countryEntityRepository.findAll(combinedSpec, pageRequest);
    }

    @Override
    public CountryEntity getCountry(String isoKey) {
        return countryEntityRepository.findByIdWithCurrencies(isoKey).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", isoKey);
            return entity;
        }).orElse(null);
    }
}
