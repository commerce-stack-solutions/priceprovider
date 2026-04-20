package io.commercestacksolutions.priceproviderservice.service.group;

import io.commercestacksolutions.commons.permissionselector.PermissionFilterBuilder;
import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
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
    private final PermissionFilterBuilder permissionFilterBuilder;
    private final AuthorizationContext authorizationContext;

    @Autowired
    public GroupServiceImpl(
            GroupEntityRepository groupEntityRepository,
            List<ValidationRule<GroupEntity>> validationRules,
            PermissionFilterBuilder permissionFilterBuilder,
            AuthorizationContext authorizationContext) {
        this.groupEntityRepository = groupEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(GroupEntity.class);
        this.permissionFilterBuilder = permissionFilterBuilder;
        this.authorizationContext = authorizationContext;
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
        resolvePathBasedRefs(groupEntity);
        validateEntity(groupEntity);
        updateAuditTimestamps(groupEntity);
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

        // Build specification from permission selectors
        Set<AppPermissionEntity> permissions = authorizationContext.getCurrentPermissions();
        Specification<GroupEntity> permissionSpec = permissionFilterBuilder.buildFilter(permissions, "Group", "read");

        // Build specification from user query (if provided)
        Specification<GroupEntity> querySpec = null;
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            querySpec = SpecificationBuilder.build(expression);
        }

        // Combine specifications
        Specification<GroupEntity> combinedSpec;
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
            return groupEntityRepository.findAll(pageRequest);
        }

        return groupEntityRepository.findAll(combinedSpec, pageRequest);
    }

    public Optional<GroupEntity> getGroupById(String id) {
        return groupEntityRepository.findById(id);
    }

    public GroupEntity getGroup(String id) {
        return groupEntityRepository.findById(id).orElse(null);
    }

    public GroupEntity getGroupByPath(String path) {
        return groupEntityRepository.findByPath(path).orElse(null);
    }

    public GroupEntity updateGroup(GroupEntity updatedGroup) throws EntityValidationException {
        return save(updatedGroup);
    }

    public void deleteGroup(String id) {
        groupEntityRepository.deleteById(id);
    }
}
