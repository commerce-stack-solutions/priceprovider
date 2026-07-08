package io.commercestacksolutions.priceproviderservice.service.country;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.SpecificationCombiner;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.QueryReflectionUtil;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.CountryEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    private final SpecificationCombiner specificationCombiner;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;
    private final EntityManager entityManager;

    @Autowired
    public CountryServiceImpl(
            CountryEntityRepository countryEntityRepository,
            List<ValidationRule<CountryEntity>> validationRules,
            SpecificationCombiner specificationCombiner,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService,
            EntityManager entityManager) {
        this.countryEntityRepository = countryEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        // Create a QueryParser configured with the entity's field types so parser can validate types
        this.queryParser = new QueryParser(QueryReflectionUtil.buildFieldTypeMap(CountryEntity.class));
        this.specificationCombiner = specificationCombiner;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
        this.entityManager = entityManager;
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
    public <ID> JpaRepository<CountryEntity, ID> getRepository() {
        @SuppressWarnings("unchecked")
        JpaRepository<CountryEntity, ID> repo = (JpaRepository<CountryEntity, ID>) countryEntityRepository;
        return repo;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public EntityAuthorizationService getEntityAuthorizationService() {
        return entityAuthorizationService;
    }

    @Override
    public <ID> ID extractEntityId(CountryEntity entity) {
        @SuppressWarnings("unchecked")
        ID id = (ID) entity.getIsoKey();
        return id;
    }

    @Override
    public CountryEntity save(CountryEntity countryEntity) throws EntityValidationException {
        return performGenericSave(countryEntity);
    }

    @Override
    public void deleteCountry(String isoKey) {
        countryEntityRepository.findById(isoKey).ifPresent(entity -> {
            // Check delete permission on the existing entity (before deletion)
            entityAuthorizationService.checkAccessBeforeAndAfter(
                entity,
                null,  // No "after" state for delete
                getEntityTypeName(),
                "delete",
                isoKey
            );
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

        // Combine permission-based and query-based filtering
        Specification<CountryEntity> combinedSpec = specificationCombiner.combine(
                authorizationContext.getCurrentPermissions(), "Country", "read", query, queryParser);

        if (combinedSpec != null) {
            return countryEntityRepository.findAll(combinedSpec, pageRequest);
        } else {
            // No filters at all (global permission, no query)
            return countryEntityRepository.findAll(pageRequest);
        }
    }

    @Override
    public CountryEntity getCountry(String isoKey) {
        return countryEntityRepository.findByIdWithCurrencies(isoKey).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", isoKey);
            return entity;
        }).orElse(null);
    }
}
