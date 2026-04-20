package io.commercestacksolutions.priceproviderservice.service.channel;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.PermissionFilterBuilder;
import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.ChannelEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
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
 * Implementation of ChannelService interface.
 * This class provides concrete implementation for channel management operations.
 */
@Service
public class ChannelServiceImpl implements ChannelService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final ChannelEntityRepository channelEntityRepository;
    private final EntityValidator<ChannelEntity> entityValidator;
    private final QueryParser queryParser;
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;

    @Autowired
    public ChannelServiceImpl(
            ChannelEntityRepository channelEntityRepository,
            List<ValidationRule<ChannelEntity>> validationRules,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService) {
        this.channelEntityRepository = channelEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        // Create a QueryParser configured with the entity's field types so parser can validate types
        this.queryParser = new QueryParser(QueryReflectionUtil.buildFieldTypeMap(ChannelEntity.class));
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
    }

    @Override
    public Class<ChannelEntity> getTargetClass() {
        return ChannelEntity.class;
    }

    @Override
    public EntityValidator<ChannelEntity> getEntityValidator() {
        return entityValidator;
    }

    @Override
    public ChannelEntity save(ChannelEntity channelEntity) throws EntityValidationException {
        validateEntity(channelEntity);
        updateAuditTimestamps(channelEntity);

        // Check write permission before saving
        entityAuthorizationService.checkAccess(channelEntity, getEntityTypeName(), "write",
            channelEntity.getId() != null ? channelEntity.getId() : "new");

        return channelEntityRepository.save(channelEntity);
    }

    @Override
    public void deleteChannel(String id) {
        channelEntityRepository.findById(id).ifPresent(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "delete", id);
            channelEntityRepository.deleteById(id);
        });
    }

    @Override
    public Page<ChannelEntity> getChannels(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
        Specification<ChannelEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "Channel", "read");

        // Build specification from user query (if provided)
        Specification<ChannelEntity> querySpec = null;
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            querySpec = SpecificationBuilder.build(expression);
        }

        // Combine specifications
        Specification<ChannelEntity> combinedSpec;
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
            return channelEntityRepository.findAll(pageRequest);
        }

        return channelEntityRepository.findAll(combinedSpec, pageRequest);
    }

    @Override
    public ChannelEntity getChannel(String id) {
        return channelEntityRepository.findById(id).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", id);
            return entity;
        }).orElse(null);
    }
}
