package io.commercestacksolutions.priceproviderservice.service.group;

import io.commercestacksolutions.commons.permissionselector.SpecificationCombiner;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of GroupService interface.
 * This class provides concrete implementation for group management operations.
 */
@Service
public class GroupServiceImpl implements GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    private final GroupEntityRepository groupEntityRepository;
    private final EntityValidator<GroupEntity> entityValidator;
    private final QueryParser queryParser;
    private final SpecificationCombiner specificationCombiner;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;
    private final EntityManager entityManager;

    @Autowired
    public GroupServiceImpl(
            GroupEntityRepository groupEntityRepository,
            List<ValidationRule<GroupEntity>> validationRules,
            SpecificationCombiner specificationCombiner,
            AuthorizationContext authorizationContext,
            EntityAuthorizationService entityAuthorizationService,
            EntityManager entityManager) {
        this.groupEntityRepository = groupEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(GroupEntity.class);
        this.specificationCombiner = specificationCombiner;
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
        this.entityManager = entityManager;
    }

    @Override
    public Class<GroupEntity> getTargetClass() {
        return GroupEntity.class;
    }

    @Override
    public EntityValidator<GroupEntity> getEntityValidator() {
        return entityValidator;
    }

    public GroupEntity save(GroupEntity groupEntity) throws EntityValidationException {
        // Fetch and detach existing entity for permission check
        // Note: This will clear the persistence context, detaching groupEntity
        GroupEntity existingEntity = fetchAndDetachExistingEntity(
            groupEntity.getId(), groupEntityRepository, entityManager);

        // Re-attach the incoming entity to the persistence context
        // This is necessary because fetchAndDetachExistingEntity clears the context
        if (groupEntity.getId() != null) {
            groupEntity = entityManager.merge(groupEntity);
        }

        resolvePathBasedRefs(groupEntity);
        validateEntity(groupEntity);
        updateAuditTimestamps(groupEntity);

        // Check write permission on both before (existing) and after (new) states
        entityAuthorizationService.checkAccessBeforeAndAfter(
            existingEntity,
            groupEntity,
            getEntityTypeName(),
            "write",
            groupEntity.getId() != null ? groupEntity.getId() : "new"
        );

        return groupEntityRepository.save(groupEntity);
    }

    /**
     * Resolves parentRefs and subRefs that only have a path set (no UUID id)
     * by looking up the full entity from the database.
     * This is required when importing JSON data where refs are given as path strings.
     */
    private void resolvePathBasedRefs(GroupEntity groupEntity) {
        if (groupEntity.getParentRefs() != null && !groupEntity.getParentRefs().isEmpty()) {
            Set<GroupEntity> resolvedParents = new HashSet<>();
            for (GroupEntity parent : groupEntity.getParentRefs()) {
                if (parent.getId() != null) {
                    resolvedParents.add(parent);
                } else if (parent.getPath() != null) {
                    groupEntityRepository.findByPath(parent.getPath()).ifPresent(resolvedParents::add);
                }
            }
            groupEntity.setParentRefs(resolvedParents);
        }
        if (groupEntity.getSubRefs() != null && !groupEntity.getSubRefs().isEmpty()) {
            Set<GroupEntity> resolvedSubs = new HashSet<>();
            for (GroupEntity sub : groupEntity.getSubRefs()) {
                if (sub.getId() != null) {
                    resolvedSubs.add(sub);
                } else if (sub.getPath() != null) {
                    groupEntityRepository.findByPath(sub.getPath()).ifPresent(resolvedSubs::add);
                }
            }
            groupEntity.setSubRefs(resolvedSubs);
        }
    }

    public List<GroupEntity> getAllGroups() {
        return groupEntityRepository.findAll();
    }

    @Override
    public Page<GroupEntity> getGroups(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
        Specification<GroupEntity> combinedSpec = specificationCombiner.combine(
                authorizationContext.getCurrentPermissions(), "Group", "read", query, queryParser);

        if (combinedSpec != null) {
            return groupEntityRepository.findAll(combinedSpec, pageRequest);
        } else {
            // No filters at all (global permission, no query)
            return groupEntityRepository.findAll(pageRequest);
        }
    }

    public Optional<GroupEntity> getGroupById(String id) {
        return groupEntityRepository.findById(id);
    }

    public GroupEntity getGroup(String id) {
        return groupEntityRepository.findById(id).map(entity -> {
            entityAuthorizationService.checkAccess(entity, getEntityTypeName(), "read", id);
            return entity;
        }).orElse(null);
    }

    public GroupEntity getGroupByPath(String path) {
        return groupEntityRepository.findByPath(path).orElse(null);
    }

    public GroupEntity updateGroup(GroupEntity updatedGroup) throws EntityValidationException {
        return save(updatedGroup);
    }

    public void deleteGroup(String id) {
        groupEntityRepository.findById(id).ifPresent(entity -> {
            // Check delete permission on the existing entity (before deletion)
            entityAuthorizationService.checkAccessBeforeAndAfter(
                entity,
                null,  // No "after" state for delete
                getEntityTypeName(),
                "delete",
                id
            );
            groupEntityRepository.deleteById(id);
        });
    }
}
