package io.commercestacksolutions.priceproviderservice.service.group;

import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
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
import java.util.UUID;

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

    @Autowired
    public GroupServiceImpl(GroupEntityRepository groupEntityRepository, List<ValidationRule<GroupEntity>> validationRules) {
        this.groupEntityRepository = groupEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(GroupEntity.class);
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

        // Parse and apply query filter if provided
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            Specification<GroupEntity> spec = SpecificationBuilder.build(expression);
            return groupEntityRepository.findAll(spec, pageRequest);
        }

        return groupEntityRepository.findAll(pageRequest);
    }

    public Optional<GroupEntity> getGroupById(UUID id) {
        return groupEntityRepository.findById(id);
    }

    public GroupEntity getGroup(UUID id) {
        return groupEntityRepository.findById(id).orElse(null);
    }

    public GroupEntity getGroupByPath(String path) {
        return groupEntityRepository.findByPath(path).orElse(null);
    }

    public GroupEntity updateGroup(GroupEntity updatedGroup) throws EntityValidationException {
        return save(updatedGroup);
    }

    public void deleteGroup(UUID id) {
        groupEntityRepository.deleteById(id);
    }
}
