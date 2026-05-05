package io.commercestacksolutions.priceproviderservice.service.channel;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.permissionselector.SpecificationCombiner;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.QueryReflectionUtil;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.ChannelEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
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
    private final SpecificationCombiner specificationCombiner;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;
    private final EntityManager entityManager;

    @Autowired
    public ChannelServiceImpl(
            ChannelEntityRepository channelEntityRepository,
            List<ValidationRule<ChannelEntity>> validationRules,
            SpecificationCombiner specificationCombiner,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService,
            EntityManager entityManager) {
        this.channelEntityRepository = channelEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        // Create a QueryParser configured with the entity's field types so parser can validate types
        this.queryParser = new QueryParser(QueryReflectionUtil.buildFieldTypeMap(ChannelEntity.class));
        this.specificationCombiner = specificationCombiner;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
        this.entityManager = entityManager;
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

        // Fetch and detach existing entity for permission check
        ChannelEntity existingEntity = fetchAndDetachExistingEntity(
            channelEntity.getId(), channelEntityRepository, entityManager);

        // Check write permission on both before (existing) and after (new) states
        entityAuthorizationService.checkAccessBeforeAndAfter(
            existingEntity,
            channelEntity,
            getEntityTypeName(),
            "write",
            channelEntity.getId() != null ? channelEntity.getId() : "new"
        );

        return channelEntityRepository.save(channelEntity);
    }

    @Override
    public void deleteChannel(String id) {
        channelEntityRepository.findById(id).ifPresent(entity -> {
            // Check delete permission on the existing entity (before deletion)
            entityAuthorizationService.checkAccessBeforeAndAfter(
                entity,
                null,  // No "after" state for delete
                getEntityTypeName(),
                "delete",
                id
            );
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

        // Combine permission-based and query-based filtering
        Specification<ChannelEntity> combinedSpec = specificationCombiner.combine(
                authorizationContext.getCurrentPermissions(), "Channel", "read", query, queryParser);

        if (combinedSpec != null) {
            return channelEntityRepository.findAll(combinedSpec, pageRequest);
        } else {
            // No filters at all (global permission, no query)
            return channelEntityRepository.findAll(pageRequest);
        }
    }

    @Override
    public ChannelEntity getChannel(String id) {
        return channelEntityRepository.findById(id).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", id);
            return entity;
        }).orElse(null);
    }
}
