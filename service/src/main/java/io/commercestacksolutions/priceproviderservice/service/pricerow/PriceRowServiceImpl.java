package io.commercestacksolutions.priceproviderservice.service.pricerow;

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
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.channel.ChannelService;
import io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching.PriceRowMatchingContext;
import io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching.SmartMatchingStrategy;
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
 * Implementation of PriceRowService interface.
 * This class provides concrete implementation for price row management operations.
 */
@Service
public class PriceRowServiceImpl implements PriceRowService {

    private static final Logger logger = LoggerFactory.getLogger(PriceRowServiceImpl.class);

    private final PriceRowEntityRepository priceRowEntityRepository;
    private final GroupEntityRepository groupEntityRepository;
    private final EntityValidator<PriceRowEntity> entityValidator;
    private final QueryParser queryParser;
    private final ChannelService channelService;
    private final SmartMatchingStrategy smartMatchingStrategy;
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;

    @Autowired
    public PriceRowServiceImpl(
            PriceRowEntityRepository priceRowEntityRepository,
            GroupEntityRepository groupEntityRepository,
            List<ValidationRule<PriceRowEntity>> validationRules,
            ChannelService channelService,
            SmartMatchingStrategy smartMatchingStrategy,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService) {
        this.priceRowEntityRepository = priceRowEntityRepository;
        this.groupEntityRepository = groupEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(PriceRowEntity.class);
        this.channelService = channelService;
        this.smartMatchingStrategy = smartMatchingStrategy;
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
    }

    @Override
    public Class<PriceRowEntity> getTargetClass() {
        return PriceRowEntity.class;
    }

    @Override
    public EntityValidator<PriceRowEntity> getEntityValidator() {
        return entityValidator;
    }

    public PriceRowEntity save(PriceRowEntity priceRowEntity) throws EntityValidationException {
        resolvePathBasedGroupRefs(priceRowEntity);
        validateEntity(priceRowEntity);
        updateAuditTimestamps(priceRowEntity);

        // Check write permission before saving
        entityAuthorizationService.checkAccess(priceRowEntity, getEntityTypeName(), "write",
            priceRowEntity.getId() != null ? priceRowEntity.getId() : "new");

        return priceRowEntityRepository.save(priceRowEntity);
    }

    /**
     * Resolves groupRefs that only have a path set (no UUID id) by looking up the
     * full GroupEntity from the database. This is necessary when importing JSON data
     * where groupRefs are given as path strings (e.g. "DEMO-GROUP-STANDARD") and
     * Jackson creates stub GroupEntity objects with only path set but no UUID.
     * Without this resolution JPA would try to use a null UUID as the FK in the
     * price_row_groups join table.
     */
    private void resolvePathBasedGroupRefs(PriceRowEntity priceRowEntity) {
        Set<GroupEntity> groups = priceRowEntity.getGroups();
        if (groups == null || groups.isEmpty()) {
            return;
        }
        Set<GroupEntity> resolved = new HashSet<>();
        for (GroupEntity group : groups) {
            if (group.getId() != null) {
                resolved.add(group);
            } else if (group.getPath() != null) {
                Optional<GroupEntity> found = groupEntityRepository.findByPath(group.getPath());
                if (found.isPresent()) {
                    resolved.add(found.get());
                } else {
                    logger.warn("Could not resolve group reference by path '{}' — skipping this group assignment", group.getPath());
                }
            }
        }
        priceRowEntity.setGroups(resolved);
    }

    public List<PriceRowEntity> findAll() {
        try {
            Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
            Specification<PriceRowEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "PriceRow", "read");

            if (permissionSpec != null) {
                return priceRowEntityRepository.findAll(permissionSpec);
            } else {
                return priceRowEntityRepository.findAll();
            }
        } catch (InvalidParameterException e) {
            logger.error("Error applying permission filter", e);
            return priceRowEntityRepository.findAll();
        }
    }

    public Page<PriceRowEntity> findAll(int page, int pageSize) {
        try {
            Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
            Specification<PriceRowEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "PriceRow", "read");

            PageRequest pageRequest = PageRequest.of(page, pageSize);

            if (permissionSpec != null) {
                return priceRowEntityRepository.findAll(permissionSpec, pageRequest);
            } else {
                return priceRowEntityRepository.findAll(pageRequest);
            }
        } catch (InvalidParameterException e) {
            logger.error("Error applying permission filter", e);
            return priceRowEntityRepository.findAll(PageRequest.of(page, pageSize));
        }
    }

    @Override
    public Page<PriceRowEntity> findAll(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
        Specification<PriceRowEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "PriceRow", "read");

        // Build specification from user query (if provided)
        Specification<PriceRowEntity> querySpec = null;
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            querySpec = SpecificationBuilder.build(expression);
        }

        // Combine specifications
        Specification<PriceRowEntity> combinedSpec;
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
            return priceRowEntityRepository.findAll(pageRequest);
        }

        return priceRowEntityRepository.findAll(combinedSpec, pageRequest);
    }

    public Optional<PriceRowEntity> findById(String id) {
        Optional<PriceRowEntity> entity = priceRowEntityRepository.findById(id);
        entity.ifPresent(e -> entityAuthorizationService.checkAccess(e, getEntityTypeName(), "read", e.getId()));
        return entity;
    }

    public void deleteById(String id) {
        Optional<PriceRowEntity> entity = priceRowEntityRepository.findById(id);
        if (entity.isPresent()) {
            entityAuthorizationService.checkAccess(entity.get(), getEntityTypeName(), "delete", id);
            priceRowEntityRepository.deleteById(id);
        }
    }

    @Override
    public Optional<PriceRowEntity> findByMatchingFields(PriceRowMatchingContext context) {
        return smartMatchingStrategy.findMatch(context);
    }
}