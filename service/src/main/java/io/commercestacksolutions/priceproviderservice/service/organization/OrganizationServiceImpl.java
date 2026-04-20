package io.commercestacksolutions.priceproviderservice.service.organization;

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
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.OrganizationEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of OrganizationService interface.
 * This class provides concrete implementation for organization management operations.
 */
@Service
public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    private final OrganizationEntityRepository organizationEntityRepository;
    private final GroupEntityRepository groupEntityRepository;
    private final EntityValidator<OrganizationEntity> entityValidator;
    private final QueryParser queryParser;
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;

    @Autowired
    public OrganizationServiceImpl(OrganizationEntityRepository organizationEntityRepository,
                                   GroupEntityRepository groupEntityRepository,
                                   List<ValidationRule<OrganizationEntity>> validationRules,
                                   PermissionFilterBuilder permissionFilterBuilder,
                                   AuthorizationContext authorizationContext,
                                   EntityAuthorizationService entityAuthorizationService) {
        this.organizationEntityRepository = organizationEntityRepository;
        this.groupEntityRepository = groupEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(OrganizationEntity.class);
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
    }

    @Override
    public Class<OrganizationEntity> getTargetClass() {
        return OrganizationEntity.class;
    }

    @Override
    public EntityValidator<OrganizationEntity> getEntityValidator() {
        return entityValidator;
    }

    public OrganizationEntity save(OrganizationEntity organizationEntity) throws EntityValidationException {
        resolvePathBasedRefs(organizationEntity);
        validateEntity(organizationEntity);
        updateAuditTimestamps(organizationEntity);

        // Check write permission before saving
        entityAuthorizationService.checkAccess(organizationEntity, getEntityTypeName(), "write",
            organizationEntity.getId() != null ? organizationEntity.getId() : "new");

        return organizationEntityRepository.save(organizationEntity);
    }

    /**
     * Resolves parentRefs that only have a path set (no UUID id)
     * by looking up the full entity from the database.
     */
    private void resolvePathBasedRefs(OrganizationEntity organizationEntity) {
        if (organizationEntity.getParentRefs() != null && !organizationEntity.getParentRefs().isEmpty()) {
            Set<GroupEntity> resolvedParents = new HashSet<>();
            for (GroupEntity parent : organizationEntity.getParentRefs()) {
                if (parent.getId() != null) {
                    resolvedParents.add(parent);
                } else if (parent.getPath() != null) {
                    groupEntityRepository.findByPath(parent.getPath()).ifPresent(resolvedParents::add);
                }
            }
            organizationEntity.setParentRefs(resolvedParents);
        }
    }

    public List<OrganizationEntity> getAllOrganizations() {
        return organizationEntityRepository.findAll();
    }

    @Override
    public Page<OrganizationEntity> getOrganizations(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
        Specification<OrganizationEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "Organization", "read");

        // Build specification from user query (if provided)
        Specification<OrganizationEntity> querySpec = null;
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            querySpec = SpecificationBuilder.build(expression);
        }

        // Combine specifications
        Specification<OrganizationEntity> combinedSpec;
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
            return organizationEntityRepository.findAll(pageRequest);
        }

        return organizationEntityRepository.findAll(combinedSpec, pageRequest);
    }

    public Optional<OrganizationEntity> getOrganizationById(String id) {
        return organizationEntityRepository.findById(id);
    }

    public OrganizationEntity getOrganization(String id) {
        return organizationEntityRepository.findById(id).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", id);
            return entity;
        }).orElse(null);
    }

    public OrganizationEntity getOrganizationByPath(String path) {
        return organizationEntityRepository.findByPath(path).orElse(null);
    }

    public OrganizationEntity updateOrganization(OrganizationEntity updatedOrganization) throws EntityValidationException {
        return save(updatedOrganization);
    }

    public void deleteOrganization(String id) {
        organizationEntityRepository.findById(id).ifPresent(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "delete", id);
            organizationEntityRepository.deleteById(id);
        });
    }
}
