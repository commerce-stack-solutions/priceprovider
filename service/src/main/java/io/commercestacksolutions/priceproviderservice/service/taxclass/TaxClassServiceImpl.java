package io.commercestacksolutions.priceproviderservice.service.taxclass;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.PermissionFilterBuilder;
import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
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
 * Implementation of TaxClassService interface.
 * This class provides concrete implementation for tax class management operations.
 */
@Service
public class TaxClassServiceImpl implements TaxClassService {

    private static final Logger logger = LoggerFactory.getLogger(TaxClassServiceImpl.class);

    private final TaxClassEntityRepository taxClassEntityRepository;
    private final EntityValidator<TaxClassEntity> entityValidator;
    private final QueryParser queryParser;
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;

    @Autowired
    public TaxClassServiceImpl(
            TaxClassEntityRepository taxClassEntityRepository,
            List<ValidationRule<TaxClassEntity>> validationRules,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService) {
        this.taxClassEntityRepository = taxClassEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        // Create a QueryParser configured with the entity's field types so parser can validate types
        this.queryParser = new QueryParser(QueryReflectionUtil.buildFieldTypeMap(TaxClassEntity.class));
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
    }

    @Override
    public Class<TaxClassEntity> getTargetClass() {
        return TaxClassEntity.class;
    }

    @Override
    public EntityValidator<TaxClassEntity> getEntityValidator() {
        return entityValidator;
    }

    public TaxClassEntity save(TaxClassEntity taxClassEntity) throws EntityValidationException {
        validateEntity(taxClassEntity);
        updateAuditTimestamps(taxClassEntity);

        // Check write permission before saving
        entityAuthorizationService.checkAccess(taxClassEntity, getEntityTypeName(), "write",
            taxClassEntity.getTaxClassId() != null ? taxClassEntity.getTaxClassId() : "new");

        return taxClassEntityRepository.save(taxClassEntity);
    }

    public void deleteTaxClass(String taxClassId) {
        taxClassEntityRepository.findById(taxClassId).ifPresent(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "delete", taxClassId);
            taxClassEntityRepository.deleteById(taxClassId);
        });
    }


    @Override
    public Page<TaxClassEntity> getTaxClasses(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
        Specification<TaxClassEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "TaxClass", "read");

        // Build specification from user query (if provided)
        Specification<TaxClassEntity> querySpec = null;
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            querySpec = SpecificationBuilder.build(expression);
        }

        // Combine specifications
        Specification<TaxClassEntity> combinedSpec;
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
            return taxClassEntityRepository.findAll(pageRequest);
        }

        return taxClassEntityRepository.findAll(combinedSpec, pageRequest);
    }

    public TaxClassEntity getTaxClass(String taxClassId) {
        return taxClassEntityRepository.findById(taxClassId).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", taxClassId);
            return entity;
        }).orElse(null);
    }

}
